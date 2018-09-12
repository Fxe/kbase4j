package me.fxe.report.kbase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.MetaboliteMajorLabel;
import pt.uminho.sysbio.biosynth.integration.io.dao.neo4j.ReactionMajorLabel;
import pt.uminho.sysbio.biosynthframework.Dataset;
import pt.uminho.sysbio.biosynthframework.EntityType;
import pt.uminho.sysbio.biosynthframework.ExternalReference;
import pt.uminho.sysbio.biosynthframework.integration.model.UnifiedModel;
import pt.uminho.sysbio.biosynthframework.util.CollectionUtils;
import pt.uminho.sysbio.biosynthframework.util.DataUtils;

public class JoseModelStatsReport {
  
  public static String[] getScmpOrder() {
    String[] scmpOrder = new String[] {
        "e0", //Extracellular
        "c0", //Cytosol
        "m0", //Mitochondria
        "n0", //Nucleus
        "g0", //Golgi
        "r0", //Endoplasmic_Reticulum
        "x0", //Peroxisome
        "v0", //Vac
        "p0", //Periplasm
        "b0", //Boundary
        "others",
    };
    return scmpOrder;
  }
  
  public static void JoseFungiStatsReport(Dataset<String, String, Object> generalStats, UnifiedModel umodel, String modelEntry) {
    Map<String, Integer> spiScmpCount = new HashMap<>();
    Map<String, Integer> rxnScmpCount = new HashMap<>();
    Map<String, Integer> spiScmpMsCount = new HashMap<>();
    Map<String, Integer> rxnScmpMsCount = new HashMap<>();
    for (String e : getScmpOrder()) {
      spiScmpCount.put(e, 0);
      rxnScmpCount.put(e, 0);
      spiScmpMsCount.put(e, 0);
      rxnScmpMsCount.put(e, 0);
    }

    int genes = 0;
    int spiSize = 0;
    int spiWithModelseedSize = 0;
    int rxnSize = 0;

    int rxnTranslocSize = 0;
    int rxnTranslocGprSize = 0;
    int rxnTranslocModelseedSize = 0;

    int rxnNonTranslocSize = 0;
    int rxnNonTranslocGprSize = 0;
    int rxnNonTranslocGprModelseedSize = 0;
    int rxnNonTranslocModelseedSize = 0;

    System.out.println(umodel.geneToReaction.size());

    Set<Long> spiIdSet = umodel.getIds(modelEntry, EntityType.SPECIE);
    Set<Long> rxnIdSet = umodel.getIds(modelEntry, EntityType.MODEL_REACTION);
    Set<Long> gneIdSet = umodel.getIds(modelEntry, EntityType.GENE);

    for (long i : spiIdSet) {
      long cmpId = umodel.idToCmpId.get(i);
      String cmpEntry = umodel.entryMap.get(cmpId);
      boolean annotated = false;
      Set<ExternalReference> references = umodel.getReferences(i);
      for (ExternalReference reference : references) {
        if (reference.source.equals(MetaboliteMajorLabel.ModelSeed.toString())) {
          annotated = true;
          break;
        }
      }

      if (spiScmpCount.containsKey(cmpEntry)) {
        CollectionUtils.increaseCount(spiScmpCount, cmpEntry, 1);
      } else {
        if (!cmpEntry.startsWith("z")) {
          System.out.println(cmpEntry);
        }
        CollectionUtils.increaseCount(spiScmpCount, "others", 1);
      }

      if (annotated) {
        spiWithModelseedSize++;

        if (spiScmpMsCount.containsKey(cmpEntry)) {
          CollectionUtils.increaseCount(spiScmpMsCount, cmpEntry, 1);
        } else {
          if (!cmpEntry.startsWith("z")) {
            System.out.println(cmpEntry);
          }
          CollectionUtils.increaseCount(spiScmpMsCount, "others", 1);
        }
      }
    }

    for (long i : rxnIdSet) {
      Set<ExternalReference> references = umodel.getReferences(i);
      String gpr = umodel.idToGpr.get(i);
      boolean annotated = false;
      for (ExternalReference reference : references) {
        if (reference.source.equals(ReactionMajorLabel.ModelSeedReaction.toString())) {
          annotated = true;
          break;
        }
      }

      Set<EntityType> types = umodel.idToTypes.get(i);
      if (types == null) {
        types = new HashSet<>();
      }
      if (types.contains(EntityType.TRANSLOCATION)) {
        rxnTranslocSize++;
        if (annotated) {
          rxnTranslocModelseedSize++;
        }
        if (!DataUtils.empty(gpr)) {
          rxnTranslocGprSize++;
        }
      } else {
        Map<Long, Double> stoich = umodel.idToStoich.get(i);
        Set<String> cmpEntries = new HashSet<>();
        for (long spiId : stoich.keySet()) {
          long cmpId = umodel.idToCmpId.get(spiId);
          cmpEntries.add(umodel.entryMap.get(cmpId));
        }
        if (cmpEntries.size() != 1) {
          System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWW");
        }
        String cmpEntry = cmpEntries.iterator().next();
        if (rxnScmpCount.containsKey(cmpEntry)) {
          CollectionUtils.increaseCount(rxnScmpCount, cmpEntry, 1);
        } else {
          if (!cmpEntry.startsWith("z")) {
            System.out.println(cmpEntry);
          }
          CollectionUtils.increaseCount(rxnScmpCount, "others", 1);
        }

        rxnNonTranslocSize++;
        if (annotated) {
          rxnNonTranslocModelseedSize++;

          if (rxnScmpMsCount.containsKey(cmpEntry)) {
            CollectionUtils.increaseCount(rxnScmpMsCount, cmpEntry, 1);
          } else {
            if (!cmpEntry.startsWith("z")) {
              System.out.println(cmpEntry);
            }
            CollectionUtils.increaseCount(rxnScmpMsCount, "others", 1);
          }
        }
        if (!DataUtils.empty(gpr)) {
          rxnNonTranslocGprSize++;
          if (annotated) {
            rxnNonTranslocGprModelseedSize++;
          }
        }
      }
      //      for (ExternalReference reference : references) {
      //        if (reference.source.equals(MetaboliteMajorLabel.ModelSeed.toString())) {
      //          spiWithModelseedSize++;
      //          break;
      //        }
      //      }
    }


    genes   = gneIdSet.size();
    spiSize = spiIdSet.size();
    rxnSize = rxnIdSet.size();

    System.out.println(spiSize + " " + spiWithModelseedSize);
    System.out.println(spiScmpCount);
    System.out.println(rxnScmpCount);

    System.out.println(rxnSize + " " + rxnNonTranslocSize +  " " + rxnTranslocSize);
    System.out.println(rxnSize + " " + rxnNonTranslocModelseedSize +  " " + rxnTranslocModelseedSize);
    System.out.println(rxnSize + " " + rxnNonTranslocGprSize +  " " + rxnTranslocGprSize);


    for (String cmpEntry : spiScmpCount.keySet()) {
      int total = spiScmpCount.get(cmpEntry);
      int integrated = spiScmpMsCount.get(cmpEntry);
      generalStats.add(modelEntry, "No. of compounds at " + cmpEntry, 
          String.format("%d/%d", integrated, total));
    }

    for (String cmpEntry : rxnScmpCount.keySet()) {
      int total = rxnScmpCount.get(cmpEntry);
      int integrated = rxnScmpMsCount.get(cmpEntry);
      generalStats.add(modelEntry, "No. of non-transport reactions at " + cmpEntry, 
          String.format("%d/%d", integrated, total));
    }

    generalStats.add(modelEntry, "No. of features", genes);
    generalStats.add(modelEntry, "rxn (total)", rxnSize);

    generalStats.add(modelEntry, "% of compounds", 
        spiWithModelseedSize / (double)spiSize);
    generalStats.add(modelEntry, "No. of compounds", 
        String.format("%d/%d", spiWithModelseedSize, spiSize));
    generalStats.add(modelEntry, "% of non-transport reactions", 
        rxnNonTranslocModelseedSize / (double)rxnNonTranslocSize);
    generalStats.add(modelEntry, "No. of non-transport reactions", 
        String.format("%d/%s", rxnNonTranslocModelseedSize, rxnNonTranslocSize));


    generalStats.add(modelEntry, "% of non-transport reactions with GPRs", 
        rxnNonTranslocGprModelseedSize / (double)rxnNonTranslocGprSize);
    generalStats.add(modelEntry, "No. of non-transport reactions with GPRs", 
        String.format("%d/%s", rxnNonTranslocGprModelseedSize, rxnNonTranslocGprSize));

    //    generalStats.add(modelEntry, "No. of non-transport reactions with GPRs", rxnNonTranslocGprSize);

    generalStats.add(modelEntry, "No. of transport reactions", rxnTranslocSize);
    generalStats.add(modelEntry, "No. of transport reactions with GPRs", rxnTranslocGprSize);
  }
}
