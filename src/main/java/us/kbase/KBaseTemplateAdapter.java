package us.kbase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kbasefba.NewModelTemplate;
import kbasefba.NewTemplateReaction;
import kbasefba.TemplateComplex;
import kbasefba.TemplateComplexRole;
import kbasefba.TemplateRole;
import me.fxe.kbase.KBaseUtils;
import pt.uminho.sysbio.biosynthframework.util.CollectionUtils;

public class KBaseTemplateAdapter {
  
  private final NewModelTemplate template;
  public Map<String, TemplateComplex> cpxMap = new HashMap<>();
  public Map<String, TemplateRole> roleMap = new HashMap<>();
  public Map<String, NewTemplateReaction> trxnMap = new HashMap<>();
  public Map<String, Set<String>> rxnToTemplateReaction = new HashMap<>();
  public Map<String, Set<String>> cpxIdToTemplateReactions = new HashMap<>();
  public Map<String, Set<String>> roleIdToTemplateReactions = new HashMap<>();
  public Map<String, Set<String>> roleIdToComplexIds = new HashMap<>();
  
  public KBaseTemplateAdapter(final NewModelTemplate template) {
    this.template = template;
    
    for (TemplateComplex cpx : this.template.getComplexes()) {
      cpxMap.put(cpx.getId(), cpx);
      for (TemplateComplexRole role : cpx.getComplexroles()) {
        String roleId = KBaseUtils.getEntryFromRef(role.getTemplateroleRef());
        CollectionUtils.insertHS(roleId, cpx.getId(), roleIdToComplexIds);
      }
    }
    
    for (TemplateRole role : this.template.getRoles()) {
      roleMap.put(role.getId(), role);
    }
    
    for (NewTemplateReaction trxn : this.template.getReactions()) {
      trxnMap.put(trxn.getId(), trxn);

      String ref = trxn.getReactionRef();
      if (ref == null) { 
        String id  = trxn.getId().split("_")[0];
        CollectionUtils.insertHS(id, trxn.getId(), rxnToTemplateReaction);
      } else {
        String id = KBaseUtils.getEntryFromRef(trxn.getReactionRef());
        CollectionUtils.insertHS(id, trxn.getId(), rxnToTemplateReaction);
      }

      for (String complexRef : trxn.getTemplatecomplexRefs()) {
        String cpxId = KBaseUtils.getEntryFromRef(complexRef);
        TemplateComplex cpx = cpxMap.get(KBaseUtils.getEntryFromRef(complexRef));
        CollectionUtils.insertHS(cpxId, trxn.getId(), cpxIdToTemplateReactions);
        for (TemplateComplexRole role : cpx.getComplexroles()) {
          String roleId = KBaseUtils.getEntryFromRef(role.getTemplateroleRef());
          CollectionUtils.insertHS(roleId, trxn.getId(), roleIdToTemplateReactions);
        }
      }
    }
  }
  
  public boolean isCompatible(Map<String, Set<String>> gpr) {
    boolean compatible = true;
    
    for (String cpxId : gpr.keySet()) {
      if (!this.cpxMap.containsKey(cpxId)) {
        compatible = false;
        System.out.println(cpxId);
      }
      for (String roleId : gpr.get(cpxId)) {
        if (!this.roleMap.containsKey(roleId)) {
          compatible = false;
          System.out.println(roleId);
        }
      }
    }
    
    return compatible;
  }
  
  public Set<NewTemplateReaction> getRoleTemplateReactions() {
    return null;
  }
  
  public Set<NewTemplateReaction> getComplexTemplateReactions() {
    return null;
  }
  
  public Map<String, Set<String>> getGpr(String trxnId) {
    NewTemplateReaction trxn = trxnMap.get(trxnId);
    
    Map<String, Set<String>> gpr = new HashMap<>();
    
    for (String complexRef : trxn.getTemplatecomplexRefs()) {
      String cpxId = KBaseUtils.getEntryFromRef(complexRef);
      TemplateComplex cpx = cpxMap.get(KBaseUtils.getEntryFromRef(complexRef));
      for (TemplateComplexRole role : cpx.getComplexroles()) {
        String roleId = KBaseUtils.getEntryFromRef(role.getTemplateroleRef());
        CollectionUtils.insertHS(cpxId, roleId, gpr);
      }
    }
    
    return gpr;
  }
  
  public void describe(String rxnId) {
    for (String trxnId : rxnToTemplateReaction.get(rxnId)) {
      NewTemplateReaction trxn = trxnMap.get(trxnId);
      System.out.println(trxn);         
      for (String complexRef : trxn.getTemplatecomplexRefs()) {
        System.out.println("\t" + complexRef);
        TemplateComplex cpx = cpxMap.get(KBaseUtils.getEntryFromRef(complexRef));
        System.out.println("\t" + cpx);
        for (TemplateComplexRole role : cpx.getComplexroles()) {
          System.out.println("\t\t" + role);
        }
      }
    }
  }

  /**
   * Make GPR (cpx and roles) unique to Template Reaction
   * @param trxnId
   * @param gpr
   */
  public void lock(String trxnId, Map<String, Set<String>> gpr) {
    System.out.println(gpr);
    NewTemplateReaction trxn = trxnMap.get(trxnId);
    trxn.getTemplatecomplexRefs().clear();
    
    Map<String, Set<String>> roleToComplex = new HashMap<>();
    
    for (String cpxId : gpr.keySet()) {
      for (String roleId : gpr.get(cpxId)) {
        CollectionUtils.insertHS(roleId, cpxId, roleToComplex);
      }
      trxn.getTemplatecomplexRefs().add("~/complexes/id/" + cpxId);
      TemplateComplex cpx = this.cpxMap.get(cpxId);
      
      //remove cpx from other reactions
      for (String otherTrxnId : this.cpxIdToTemplateReactions.get(cpxId)) {
        if (!trxnId.equals(otherTrxnId)) {
          NewTemplateReaction otherTrxn = trxnMap.get(otherTrxnId);
//          System.out.println(otherTrxnId + " " + otherTrxn.getTemplatecomplexRefs().size());
          otherTrxn.getTemplatecomplexRefs().remove("~/complexes/id/" + cpxId);
//          System.out.println(otherTrxnId + " " + otherTrxn.getTemplatecomplexRefs().size());
        }
      }
      //cpx is now unique to trxnId
      
      List<TemplateComplexRole> allow = new ArrayList<>();
      for (TemplateComplexRole role : cpx.getComplexroles()) {
        String roleId = KBaseUtils.getEntryFromRef(role.getTemplateroleRef());
        if (gpr.get(cpxId).contains(roleId)) {
          allow.add(role);
        }
      }
      System.out.println(cpx.getComplexroles().size() + " " + allow.size());
      cpx.getComplexroles().clear();
      cpx.getComplexroles().addAll(allow);
    }
    
    //remove the role from all complexes
//    for (String cpxId : cpxMap.keySet()) {
//      TemplateComplex cpx = cpxMap.get(cpxId);
//      List<TemplateComplexRole> allow = new ArrayList<>();
//      for (TemplateComplexRole role : cpx.getComplexroles()) {
//        String roleId = KBaseModelAdapter.getEntryFromRef(role.getTemplateroleRef());
//        if (!roleToComplex.containsKey(roleId)) {
//          allow.add(role);
//        }
//      }
//      
//      System.out.println(cpx.getComplexroles());
//      break;
//    }
    
    for (String roleId : roleToComplex.keySet()) {
      for (String cpxId : this.roleIdToComplexIds.get(roleId)) {
        if (!roleToComplex.get(roleId).contains(cpxId)) {
          TemplateComplex cpx = this.cpxMap.get(cpxId);
          removeRoleFromTemplateComplex(cpx, roleId);
          if (cpx.getComplexroles().isEmpty()) {
            System.out.println("!!!!!!!" + cpxId + " " + cpxIdToTemplateReactions.get(cpxId));
            if (cpxIdToTemplateReactions.containsKey(cpxId)) {
              for (String otrxnId : cpxIdToTemplateReactions.get(cpxId)) {
                NewTemplateReaction otrxn = trxnMap.get(otrxnId);
                removeComplexFromTemplateComplex(otrxn, cpxId);
                System.out.println(trxnMap.get(otrxnId).getTemplatecomplexRefs());
              }
            }
          }
//          System.out.println("Remove role " + roleId + " " + cpxId);
        }
      }
    }
      
    System.out.println(trxn);
    
    
  }
  
 
  public static void removeRoleFromTemplateComplex(TemplateComplex cpx, String roleId) {
    List<TemplateComplexRole> allow = new ArrayList<>();
    for (TemplateComplexRole role : cpx.getComplexroles()) {
      if (!role.getTemplateroleRef().contains(roleId)) {
        allow.add(role);
      }
    }
    cpx.getComplexroles().clear();
    cpx.getComplexroles().addAll(allow);
  }
  
  public static void removeComplexFromTemplateComplex(NewTemplateReaction trxn, String cpxId) {
    List<String> allow = new ArrayList<>();
    for (String cpxRef : trxn.getTemplatecomplexRefs()) {
      if (!cpxRef.contains(cpxId)) {
        allow.add(cpxRef);
      }
    }
    trxn.getTemplatecomplexRefs().clear();
    trxn.getTemplatecomplexRefs().addAll(allow);
  }

  public Set<String> getReactionIds() {
    return new HashSet<String>(this.rxnToTemplateReaction.keySet());
  }

  public void removeReaction(String rxnId) {
    if (rxnToTemplateReaction.containsKey(rxnId)) {
      for (String trxnId : rxnToTemplateReaction.get(rxnId)) {
        NewTemplateReaction trxn = trxnMap.get(trxnId);
//        System.out.println(trxn.getTemplatecomplexRefs());
        trxn.getTemplatecomplexRefs().clear();
//        System.out.println(trxnId);
      }
    }
  }
  
  public void cleanOphanTemplateReactions() {
    List<NewTemplateReaction> keep = new ArrayList<>();
    for (NewTemplateReaction trxn : template.getReactions()) {
      if (!trxn.getTemplatecomplexRefs().isEmpty()) {
        keep.add(trxn);
      }
    }
    
    System.out.println(keep.size() + " " + template.getReactions().size());
    template.setReactions(keep);
  }
  
  public void cleanOphanComplexes() {
    
    
    Set<String> assigned = new HashSet<>();
    for (NewTemplateReaction trxn : template.getReactions()) {
      for (String ref : trxn.getTemplatecomplexRefs()) {
        String cpxId = KBaseUtils.getEntryFromRef(ref);
        assigned.add(cpxId);
      }
    }
    
    List<TemplateComplex> keep = new ArrayList<>();
    for (TemplateComplex cpx : template.getComplexes()) {
      if (!assigned.contains(cpx.getId())) {
        keep.add(cpx);
      }
    }
    
    System.out.println(keep.size() + " " + template.getComplexes().size());
    template.setComplexes(keep);
  }

  public Set<String> getFunctions() {
    Set<String> functions = new HashSet<>();
    for (TemplateRole role : template.getRoles()) {
      functions.add(role.getName());
    }
    return functions;
  }

  public void removeFunctionFromComplexes(String f) {
    
    String roleId = null;
    
    for (TemplateRole role : this.roleMap.values()) {
      if (role.getName().equals(f)) {
        roleId = role.getId();
      }
    }
    
    if (roleId != null) {
      for (String cpxId : this.cpxMap.keySet()) {
        TemplateComplex cpx = this.cpxMap.get(cpxId);
        removeRoleFromTemplateComplex(cpx, roleId);
      }
    } else {
      System.out.println(f);
    }

    
  }
}
