package me.fxe.kbase.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kbasefba.FBAModel;
import kbasefba.ModelCompartment;
import kbasefba.ModelCompound;
import kbasefba.ModelReaction;
import me.fxe.kbase.KBaseModelAdapter;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.ReactionMajorLabel;
import pt.uminho.sysbio.biosynthframework.EntityType;
import pt.uminho.sysbio.biosynthframework.Range;
import pt.uminho.sysbio.biosynthframework.integration.model.UnifiedModel;
import pt.uminho.sysbio.biosynthframework.integration.model.UnifiedModelBuilder;
import pt.uminho.sysbio.biosynthframework.util.DataUtils;
import pt.uminho.sysbio.biosynthframework.util.GprUtils;

public class KbaseUnifiedModelBuilder implements UnifiedModelBuilder {

  private static final Logger logger = LoggerFactory.getLogger(KbaseUnifiedModelBuilder.class);

  private final FBAModel fbaModel;
  private final String modelEntry;
  private final KBaseModelAdapter adapter;

  public KbaseUnifiedModelBuilder(final FBAModel fbaModel,
                                  final String modelEntry) {
    this.fbaModel = fbaModel;
    this.modelEntry = modelEntry;
    this.adapter = new KBaseModelAdapter(this.fbaModel);
  }

  @Override
  public void setupCompartments() {
    // TODO Auto-generated method stub

  }

  @Override
  public void setupSpecies() {
    // TODO Auto-generated method stub

  }

  @Override
  public void setupReactions() {

  }

  @Override
  public void setupCompartments(UnifiedModel umodel) {
    for (ModelCompartment xcmp : fbaModel.getModelcompartments()) {
      String cmpEntry = xcmp.getId();
      String name = xcmp.getLabel();
      umodel.registerModelCompartment(modelEntry, cmpEntry, name, null);
    }
  }

  @Override
  public void setupSpecies(UnifiedModel umodel) {
    for (ModelCompound kspi : fbaModel.getModelcompounds()) {
      String spiEntry = kspi.getId();
      String cmpEntry = KBaseModelAdapter.getEntryFromRef(
          kspi.getModelcompartmentRef());
      String name = kspi.getName();
      boolean boundary = adapter.isBoundarySpecie(spiEntry);
      Long spiId = null; 
      if (!DataUtils.empty(spiEntry)) {
        logger.debug("[{}] [ADD] [SPECIE] {}", modelEntry, spiEntry);
        spiId = umodel.registerSpecie(modelEntry, spiEntry, cmpEntry, name);
      }
      
      Map<MetaboliteMajorLabel, String> references = new HashMap<>();
      for (String database : kspi.getDblinks().keySet()) {
        for (String cpdEntry : kspi.getDblinks().get(database)) {
          references.put(MetaboliteMajorLabel.valueOf(database), cpdEntry);
        }
      }
      umodel.registerSpecieAnnoation(modelEntry, spiEntry, references);
      
      umodel.idToBoundary.put(spiId, boundary);
      
      
//      for (XmlObject annotation : annotations) {
////        System.out.println(annotation);
//        String resource = annotation.getAttributes().get("resource");
//        ExternalReference reference = identifiersDotOrg(resource);
//        if (reference != null) {
//          references.put(MetaboliteMajorLabel.valueOf(reference.source), reference.entry);
//        }
////        if (!DataUtils.empty(resource) && 
////            resource.startsWith("http://identifiers.org/kegg.compound/")) {
////          references.put(
////              MetaboliteMajorLabel.LigandCompound, 
////              resource.replace("http://identifiers.org/kegg.compound/", ""));
////        } else if (resource.startsWith("http://identifiers.org/chebi/CHEBI:")) {
////          references.put(
////              MetaboliteMajorLabel.ChEBI, 
////              resource.replace("http://identifiers.org/chebi/CHEBI:", ""));
////        } else {
//////          System.out.println(resource);
////        }
//      }
      
      
//      System.out.println(spiEntry + " " + name + " " + references);
      
    }
  }

  @Override
  public void setupReactions(UnifiedModel umodel) {
    Map<Map<String, Double>, Set<Long>> dup = new HashMap<>(); 

    Set<String> gprStr = new HashSet<>();
    for (ModelReaction krxn : fbaModel.getModelreactions()) {
      String mrxnEntry = krxn.getId();      
      String name = krxn.getName();
      
      if (!DataUtils.empty(mrxnEntry)) {
        logger.debug("[{}] [ADD] [REACTION] {}", modelEntry, mrxnEntry);
        long mrxnId = umodel.registerModelReaction(modelEntry, mrxnEntry);
        
        Map<String, Double> stoich = adapter.getStoichiometry(mrxnEntry);
        String gpr = krxn.getImportedGpr();
        if (!DataUtils.empty(gpr)) {
          gprStr.add(gpr);
        }
        
        
        Range bounds = adapter.getBounds(mrxnEntry);
        double lb = bounds.lb;
        double ub = bounds.ub;
        if (bounds.lb < -10000) {
          lb = -10000;
        }
        if (bounds.ub >  10000) {
          ub = 10000;
        }
        bounds = new Range(lb, ub);
        System.out.println(krxn.getId() + " " + krxn.getDblinks());
        
        umodel.idToBound.put(mrxnId, bounds);
        
        Map<ReactionMajorLabel, String> references = new HashMap<>();
        for (String database : krxn.getDblinks().keySet()) {
          for (String cpdEntry : krxn.getDblinks().get(database)) {
            references.put(ReactionMajorLabel.valueOf(database), cpdEntry);
          }
        }
        umodel.registerModelReactionAnnoation(modelEntry, mrxnId, references);
        
        if (!DataUtils.empty(name)) {
          umodel.idToName.put(mrxnId, name);
        }
        
        if (!DataUtils.empty(gpr)) {
          umodel.idToGpr.put(mrxnId, gpr);
        }
        
        Map<Long, Double> istoich = new HashMap<>();
        Set<Long> cmpIds = new HashSet<>();
        for (String spiEntry : stoich.keySet()) {
          Long spiId = umodel.getId(spiEntry, modelEntry, EntityType.SPECIE);
          if (spiId == null) {
            logger.warn("species not found: {}", spiEntry);
          }
          istoich.put(spiId, stoich.get(spiEntry));
          Long cmpId = umodel.idToCmpId.get(spiId);
          cmpIds.add(cmpId);
        }
        
        if (cmpIds.size() > 1) {
          umodel.addClassToObject(mrxnId, EntityType.TRANSLOCATION);
        }
//        for (XmlObject o : xrxn.getListOfReactants()) {
//          String s = o.getAttributes().get("species");
//
//          long spiId = umodel.getId(s, modelEntry, EntityType.SPECIE);
//          String valueStr = o.getAttributes().get("stoichiometry");
//          if (valueStr == null) {
//            valueStr = "1";
//          }
//          double value = 1.0;
//          if (NumberUtils.isParsable(valueStr)) {
//            value = Double.parseDouble(valueStr);
//          }
//          istoich.put(spiId, -1 * value);
//        }
//        for (XmlObject o : xrxn.getListOfProducts()) {
//          String s = o.getAttributes().get("species");
////          System.out.println(mrxnEntry + " " + s);
//          long spiId = umodel.getId(s, modelEntry, EntityType.SPECIE);
//          String valueStr = o.getAttributes().get("stoichiometry");
//          if (valueStr == null) {
//            valueStr = "1";
//          }
//          double value = 1.0;
//          if (NumberUtils.isParsable(valueStr)) {
//            value = Double.parseDouble(valueStr);
//          }
//          istoich.put(spiId, value);
//        }

        umodel.idToStoich.put(mrxnId, istoich);
//        System.out.println(mrxnEntry + " " + bounds);
        if (!dup.containsKey(stoich)) {
          dup.put(stoich, new HashSet<Long>());
        }
        dup.get(stoich).add(mrxnId);
      }
    }
    
    Set<String> genes = new HashSet<>();
    for (String gpr : gprStr) {
      if (!DataUtils.empty(gpr)) {
        Set<String> g = GprUtils.getGenes(gpr);
        if (g != null) {
          genes.addAll(g);          
        }
      }
    }
    for (String g : genes) {
      if (!DataUtils.empty(g)) {
        umodel.registerObject(g, modelEntry, EntityType.GENE);        
      }
    }
    
    for (Map<String, Double> k : dup.keySet()) {
      Set<Long> dups = dup.get(k);
      if (dups.size() > 1) {
        umodel.duplicates(dups);
      }
    }
    

    
//    Set<String> geneSpecies = adapter.xspiType.bget(EntityType.GENE);
//    if (geneSpecies == null) {
//      geneSpecies = new HashSet<>();
//    }
//    for (long spiId : umodel.getIds(modelEntry, EntityType.SPECIE)) {
//      String entry = umodel.entryMap.get(spiId);
//      String name = umodel.idToName.get(spiId);
//      boolean inGrpStrs = false;
//      for (String str : gprStr) {
//        if (str.contains(name)) {
//          inGrpStrs = true;
//          break;
//        }
//      }
//      if (geneSpecies.contains(entry) || 
//          ((entry.startsWith("Cx_") || entry.startsWith("E_")) && inGrpStrs)) {
//        logger.debug("set object to GENE: [{}:{}]{}", spiId, null, entry);
//        umodel.addClassToObject(spiId, EntityType.GENE);
//      }
//    }
    
//    for (String entry : geneSpecies) {
//      long id = umodel.getId(entry, modelEntry);
//      EntityType baseType = umodel.typeMap.get(id);
//
//    }
    
    for (long mrxnId : umodel.getIds(modelEntry, EntityType.MODEL_REACTION)) {
      Map<Long, ?> stoich = umodel.idToStoich.get(mrxnId);
      int geneCount = 0;
//      System.out.println(umodel.entryMap.get(mrxnId) + " " + geneCount + " " + stoich.size() + " " + stoich);
      for (long spiId : stoich.keySet()) {
        Set<EntityType> types = umodel.idToTypes.get(spiId);
//        System.out.println(spiId + " " + umodel.entryMap.get(spiId) + " " + types);
        if (types != null && types.contains(EntityType.GENE)) {
          geneCount++;
        }
      }
      
      if (geneCount == stoich.size()) {
        logger.debug("set object to GENE: [{}:{}]{}", mrxnId, null, umodel.entryMap.get(mrxnId));
        umodel.addClassToObject(mrxnId, EntityType.GENE);
      }
    }

  }

}
