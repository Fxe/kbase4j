package me.fxe.kbase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kbasefba.Biomass;
import kbasefba.FBAModel;
import kbasefba.ModelCompartment;
import kbasefba.ModelCompound;
import kbasefba.ModelGapfill;
import kbasefba.ModelGapgen;
import kbasefba.ModelReaction;
import kbasefba.ModelReactionProtein;
import kbasefba.ModelReactionProteinSubunit;
import kbasefba.ModelReactionReagent;
import kbasegenomes.Feature;
import kbasegenomes.Genome;
import pt.uminho.sysbio.biosynthframework.BFunction;
import pt.uminho.sysbio.biosynthframework.EntityType;
import pt.uminho.sysbio.biosynthframework.ModelAdapter;
import pt.uminho.sysbio.biosynthframework.MultiNodeTree;
import pt.uminho.sysbio.biosynthframework.Range;
import pt.uminho.sysbio.biosynthframework.SimpleModelReaction;
import pt.uminho.sysbio.biosynthframework.SimpleModelSpecie;
import pt.uminho.sysbio.biosynthframework.SubcellularCompartment;
import pt.uminho.sysbio.biosynthframework.integration.model.IntegrationMap;
import pt.uminho.sysbio.biosynthframework.sbml.SbmlNotesParser;
import pt.uminho.sysbio.biosynthframework.sbml.XmlObject;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlCompartment;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlModel;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlModelAdapter;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlReaction;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlSpecie;
import pt.uminho.sysbio.biosynthframework.util.CollectionUtils;
import pt.uminho.sysbio.biosynthframework.util.DataUtils;
import pt.uminho.sysbio.biosynthframework.util.GprUtils;
import pt.uminho.sysbio.biosynthframework.util.SbmlUtils;

public class KBaseFBAModelFactory {

  private static final Logger logger = LoggerFactory.getLogger(KBaseFBAModelFactory.class);
  
//  private static final String DEFAULT_COMPOUND_REF = "~/template/compounds/id/cpd00000";
  private static final String MODEL_SEED_COMPOUND_REF_PATTERN = "~/template/compounds/id/%s";
  
  /**
   * KBase Genome ID<br>null -> empty genome
   */
  private String genomeRef = null;
  
  private XmlSbmlModel xmodel;
  private String modelId;
  private String modelName;
  private int counter = 0;
  private Set<String> biomassSet = new HashSet<> ();
  private SbmlNotesParser notesParser = new SbmlNotesParser();
  private IntegrationMap<String, String> simap = new IntegrationMap<>();
  private IntegrationMap<String, String> rimap = new IntegrationMap<>();
  private Map<String, String> spiToModelSeedReference = new HashMap<> ();
  private Map<String, String> rxnToModelSeedReference = new HashMap<> ();
  private Map<String, ModelCompound> modelCompounds = new HashMap<> ();
  private List<ModelCompartment> modelCompartments = new ArrayList<> ();
  private List<ModelReaction> modelReactions = new ArrayList<> ();
  private Map<String, SubcellularCompartment> scmpMap = new HashMap<>();
  private Map<String, Integer> cmpIndexMap = new HashMap<>();
  private String removeSpeciesPrefix = null;
  private String removeReactionPrefix = null;
  
  private Map<String, String> cmpMap = new HashMap<> ();
  
  public KBaseFBAModelFactory withGenomeRef(String genomeRef) {
    this.genomeRef = genomeRef;
    return this;
  }
  
  public KBaseFBAModelFactory withModelId(String modelId) {
    this.modelId = modelId;
    return this;
  }
  
  public KBaseFBAModelFactory withModelName(String modelName) {
    this.modelName = modelName;
    return this;
  }
  
  public KBaseFBAModelFactory withRemovePrefix(String prefix) {
    this.removeSpeciesPrefix = prefix;
    return this;
  }
  
  public KBaseFBAModelFactory withReactionIdRemovePrefix(String prefix) {
    this.removeReactionPrefix = prefix;
    return this;
  }
  
  public KBaseFBAModelFactory withBiomassIds(Collection<String> ids) {
    this.biomassSet.addAll(ids);
    return this;
  }
  
  public KBaseFBAModelFactory withMetaboliteModelSeedReference(Map<String, String> spiToModelSeedReference) {
    if (spiToModelSeedReference != null) {
      this.spiToModelSeedReference.putAll(spiToModelSeedReference);      
    }
    return this;
  }
  
  public KBaseFBAModelFactory withReactionModelSeedReference(Map<String, String> rxnToModelSeedReference) {
    if (rxnToModelSeedReference != null) {
      this.rxnToModelSeedReference.putAll(rxnToModelSeedReference);
    }
    return this;
  }
  
  public KBaseFBAModelFactory withCompartmentMapping(Map<String, SubcellularCompartment> scmpMap) {
    if (scmpMap != null) {
      this.scmpMap = new HashMap<>(scmpMap);      
    }
    return this;
  }
  
  public KBaseFBAModelFactory withSimpleModelReaction(SimpleModelReaction mrxn) {
    
    return this;
  }
  
  public KBaseFBAModelFactory withSpecieIntegration(IntegrationMap<String, String> integration) {
    if (integration != null) {
      this.simap = integration;
    }
    return this;
  }
  
  public KBaseFBAModelFactory withReactionIntegration(IntegrationMap<String, String> integration) {
    if (integration != null) {
      this.rimap = integration;
    }
    return this;
  }
  
  public KBaseFBAModelFactory withSimpleModelSpecie(SimpleModelSpecie<String> mspi) {
    return withSimpleModelSpecie(mspi, "cpd00000");
  }
  
  public KBaseFBAModelFactory withSimpleModelSpecie(SimpleModelSpecie<String> mspi, String modelSeedRef) {
    String spiEntry = mspi.id;
    String cmpEntry = mspi.compartmentId;
    String spiName = mspi.name;
    if (spiName == null || spiName.trim().isEmpty()) {
      spiName = "undefined";
    }
    Map<String, String> extraAttributes = new HashMap<> ();
    extraAttributes.put("original_id", spiEntry);
    ModelCompound cpd = new ModelCompound().withId(spiEntry)
        .withCompoundRef(String.format(MODEL_SEED_COMPOUND_REF_PATTERN, modelSeedRef))
        .withModelcompartmentRef(
            String.format("~/modelcompartments/id/%s", cmpMap.get(cmpEntry)))
        .withFormula("R")
        .withCharge(1.0)
        .withStringAttributes(extraAttributes)
        .withName(spiName);
    
    this.modelCompounds.put(spiEntry, cpd);
    return this;
  }

  public KBaseFBAModelFactory withXmlSbmlModel(XmlSbmlModel xmodel, boolean allowBoundary) {
    this.xmodel = xmodel;
//    long cmpIndex = 0;
    
    for (XmlSbmlCompartment xcmp : xmodel.getCompartments()) {
      String cmpEntry = xcmp.getAttributes().get("id");
      String cmpName = xcmp.getAttributes().get("name");
      String cmpId = KBaseUtils.toSymbol(scmpMap.get(cmpEntry));
      String cmpIdAndIndex = null;
      long cmpIndex = 0;
      if (cmpIndexMap.containsKey(cmpId)) {
        CollectionUtils.increaseCount(cmpIndexMap, cmpId, 1);
        cmpIdAndIndex = cmpId + cmpIndexMap.get(cmpId);
        cmpIndex = cmpIndexMap.get(cmpId);
      } else {
        cmpIndexMap.put(cmpId, 0);
        cmpIndex = 0;
        cmpIdAndIndex = cmpId + cmpIndexMap.get(cmpId);
      }
      logger.debug("{} - {} -> {}[{}] -> {}", cmpEntry, cmpName, cmpId, cmpIndex, cmpIdAndIndex);
      
      if (cmpName == null || cmpName.trim().isEmpty()) {
        cmpName = cmpEntry;
      }
//      String cmpId = "z";

      ModelCompartment cmp = new ModelCompartment().withId(cmpIdAndIndex)
          .withLabel(cmpName)
          .withPH(7.0)
          .withPotential(0.0)
          .withCompartmentIndex(cmpIndex)
          .withCompartmentRef("~/template/compartments/id/" + cmpId);
      modelCompartments.add(cmp);
      cmpMap.put(cmpEntry, cmpIdAndIndex);
    }
    
    for (XmlSbmlSpecie xspi : xmodel.getSpecies()) {
      Map<String, String> extraAttributes = new HashMap<> ();
      
      String spiEntry = xspi.getAttributes().get("id");
      String cmpEntry = xspi.getAttributes().get("compartment");
      String spiName = xspi.getAttributes().get("name");
      boolean boundaryCondition = false;
      extraAttributes.put("original_id", spiEntry);
      try {
        String b = xspi.getAttributes().get("boundaryCondition");
        if (b != null) {
          extraAttributes.put("boundary_condition", b);
          if (Boolean.parseBoolean(b)) {
            boundaryCondition = true;
          }
        }
      } catch (Exception e) {
        
      }
      
      if (spiName == null || spiName.trim().isEmpty()) {
        spiName = "undefined";
      }
      
      String ref = "cpd00000";
      if (spiToModelSeedReference.containsKey(spiEntry)) {
        ref = spiToModelSeedReference.get(spiEntry);
      }
      //maybe fetch formulas !
      String formula = "*";
      
      Map<String, List<String>> dblinks = new HashMap<> ();
      if (simap.containsKey(spiEntry)) {
        for (String db : simap.get(spiEntry).keySet()) {
          dblinks.put(db, new ArrayList<> (simap.get(spiEntry).get(db)));
        }
      }
      
      String spiId = spiEntry;
      if (removeSpeciesPrefix != null && spiEntry.startsWith(removeSpeciesPrefix)) {
        spiId = spiEntry.substring(removeSpeciesPrefix.length());
      }
      
      ModelCompound kspi = new ModelCompound().withId(spiId)
          .withCompoundRef(String.format(MODEL_SEED_COMPOUND_REF_PATTERN, ref))
          .withModelcompartmentRef(
              String.format("~/modelcompartments/id/%s", cmpMap.get(cmpEntry)))
          .withFormula(formula)
          .withCharge(1.0)
          .withDblinks(dblinks)
          .withStringAttributes(extraAttributes)
          .withName(spiName);
      
      if (allowBoundary || !boundaryCondition) {
        modelCompounds.put(spiEntry, kspi);
      }
    }
    
    return this;
  }
  
  public String getFbcGpr(XmlSbmlReaction xrxn, final XmlSbmlModel xmodel) {
    MultiNodeTree<Object> a = xrxn.getGpr();
    BFunction<Object, String> f = new BFunction<Object, String>() {
      
      @Override
      public String apply(Object t) {
        if (t instanceof Map) {
          @SuppressWarnings("rawtypes")
          Map<?, ?> m = (Map)t;
          if (m.containsKey("geneProduct")) {
            String geneProduct = (String) m.get("geneProduct");
            for (XmlObject o : xmodel.getListOfGeneProducts()) {
              if (o.getAttributes().get("id").equals(geneProduct)) {
                if (o.getAttributes().containsKey("label")) {
                  return o.getAttributes().get("label");
                }
                return o.getAttributes().get("name");
              }
            }
            return geneProduct;
          }
          return t.toString();
        }
        return t.toString();
      }
    };
    List<String> s = SbmlUtils.gprTreeToString(a, f);
    if (s != null && !s.isEmpty()) {
      return s.iterator().next();
    }
    
    return null;
  }
  
  public static List<ModelReactionProtein> setupModelReactionProteins(Set<Set<String>> proteins, Genome genome, boolean translateToId) {
    Map<String, Feature> fmap = new HashMap<> ();
    for (Feature f : genome.getFeatures()) {
      fmap.put(f.getId(), f);
      if (f.getAliases() != null) {
        for (List<String> t : f.getAliases()) {
          fmap.put(KBaseGenomeAdapter.getAlias(t), f);
        }
      }
    }
    
    Set<Set<String>> validProteins = new HashSet<> ();
    for (Set<String> p : proteins) {
      Set<String> validGenes = new HashSet<>();
      for (String g : p) {
        if (fmap.containsKey(g)) {
          if (translateToId) {
            validGenes.add(fmap.get(g).getId());
          } else {
            validGenes.add(g);
          }
          logger.trace("Gene found within feature set: {}", g);
        } else {
          logger.debug("Gene not found within feature set: {}", g);
        }
      }
      if (!validGenes.isEmpty()) {
        validProteins.add(validGenes);
      } else {
        logger.warn("protein discarded (no features found within genome): {}", p);
      }
    }
    return setupModelReactionProteins(validProteins);
  }
  
  public static List<ModelReactionProtein> setupModelReactionProteins(Set<String> genes, Genome genome, String genomeRef) {
    Map<String, Feature> fmap = new HashMap<> ();
    for (Feature f : genome.getFeatures()) {
      fmap.put(f.getId(), f);
      if (f.getAliases() != null) {
        for (List<String> t : f.getAliases()) {
          fmap.put(KBaseGenomeAdapter.getAlias(t), f);
        }
      }
    }
    
    Set<String> validGenes = new HashSet<> ();
    for (String g : genes) {
      if (fmap.containsKey(g)) {
        validGenes.add(fmap.get(g).getId());
        logger.debug("Gene found within feature set: {}", g);
      } else {
        logger.debug("Gene not found within feature set: {}", g);
      }
    }
    return setupModelReactionProteins(validGenes, genomeRef);
  }
  
  public static List<ModelReactionProtein> setupModelReactionProteins(Set<String> genes, String genomeRef) {
    List<ModelReactionProtein> mrpList = new ArrayList<> ();
    if (genes != null) {
      List<ModelReactionProteinSubunit> mrpsLists = new ArrayList<> (); 
      List<String> features = new ArrayList<> ();
      for (String g : genes) {
        features.add(String.format("%s/features/id/%s", genomeRef, g));
      }

      //1985/8/4/features/id/kb|g.220339.CDS.100
      ModelReactionProteinSubunit mrps = new ModelReactionProteinSubunit()
          .withFeatureRefs(features)
          .withTriggering(0L)
          .withRole("")
          .withNote("Imported GPR")
          .withOptionalSubunit(0L);
      mrpsLists.add(mrps);
      ModelReactionProtein mrp = new ModelReactionProtein()
          .withComplexRef("")
          .withModelReactionProteinSubunits(mrpsLists)
          .withNote("Imported GPR").withSource("");
      mrpList.add(mrp);
    }
    
    
    return mrpList;
  }
  
  public static List<ModelReactionProtein> setupModelReactionProteins(Set<Set<String>> proteins) {
    List<ModelReactionProtein> mrpList = new ArrayList<> ();
    if (proteins != null) {
      for (Set<String> prot : proteins) {
        List<ModelReactionProteinSubunit> subunits = new ArrayList<> ();
        for (String g : prot) {
          List<String> features = new ArrayList<> ();
          features.add(String.format("~/genome/features/id/%s", g));
          ModelReactionProteinSubunit subunit = new ModelReactionProteinSubunit()
              .withFeatureRefs(features)
              .withTriggering(0L)
              .withRole("")
              .withNote("Imported GPR")
              .withOptionalSubunit(0L);
          subunits.add(subunit);
        }
        
        ModelReactionProtein mrp = new ModelReactionProtein()
            .withComplexRef("")
            .withModelReactionProteinSubunits(subunits)
            .withNote("Imported GPR").withSource("");
        mrpList.add(mrp);
      }
    }
    
    return mrpList;
  }
  
  public ModelReactionReagent buildReagent(XmlObject o, double scale) {
    String species = o.getAttributes().get("species");
    ModelCompound kspi = modelCompounds.get(species);
    String stoich = o.getAttributes().get("stoichiometry");
    if (stoich == null) {
      stoich = "1";
    }
    double stoichiometry = Double.parseDouble(stoich);
    ModelReactionReagent r = new ModelReactionReagent()
        .withCoefficient(scale * stoichiometry)
        .withModelcompoundRef(String.format("~/modelcompounds/id/%s", kspi.getId()));
    return r;
  }

  public ModelReaction buildReaction(XmlSbmlReaction xrxn, ModelAdapter xadapter) {
    Map<String, String> extraAttributes = new HashMap<> ();
    String rxnEntry = xrxn.getAttributes().get("id");
    if (rxnEntry == null || rxnEntry.trim().isEmpty()) {
      rxnEntry = "R_rxn" + counter++;
    }
    String rxnName = xrxn.getAttributes().get("name");
    if (rxnName == null || rxnName.trim().isEmpty()) {
      rxnName = "undefined";
    }

    extraAttributes.put("original_id", rxnEntry);

    String gpr = xadapter.getGpr(rxnEntry);
    
    Set<String> genes = new HashSet<> ();
    if (DataUtils.empty(gpr)) {
      gpr = "";
    } else {
      try {
        genes = GprUtils.getGenes(gpr);
      } catch (Exception e) {
        System.out.println(gpr + ": " + e.getMessage());
      }
    }
    List<ModelReactionReagent> reagents = new ArrayList<> ();
    Set<String> reagentsCompartments = new HashSet<>();
    for (XmlObject o : xrxn.getListOfReactants()) {
      String species = o.getAttributes().get("species");
      if (modelCompounds.containsKey(species)) {
        ModelReactionReagent reagent = buildReagent(o, -1);
        ModelCompound kspi = modelCompounds.get(species);
        reagentsCompartments.add(
            KBaseModelAdapter.getEntryFromRef(
                kspi.getModelcompartmentRef()));
        reagents.add(reagent);
      } else {
        logger.info("deleted {}", species);
      }
    }

    for (XmlObject o : xrxn.getListOfProducts()) {
      String species = o.getAttributes().get("species");
      if (modelCompounds.containsKey(species)) {
        ModelReactionReagent reagent = buildReagent(o,  1);
        ModelCompound kspi = modelCompounds.get(species);
        reagentsCompartments.add(
            KBaseModelAdapter.getEntryFromRef(
                kspi.getModelcompartmentRef()));
        reagents.add(reagent);
      } else {
        logger.info("deleted {}", species);
      }
    }

    Map<String, List<String>> dblinks = new HashMap<> ();
    if (rimap.containsKey(rxnEntry)) {
      for (String db : rimap.get(rxnEntry).keySet()) {
        dblinks.put(db, new ArrayList<> (rimap.get(rxnEntry).get(db)));
      }
    }

    //    Set<String> aaa = KBaseUtils.getCompartments(reagents);
    String rxnCmp = cmpMap.values().iterator().next();
    String rxnCmpSymbol = "c";
    String rxnCmpRef = String.format("~/modelcompartments/id/%s", rxnCmp);
    if (reagentsCompartments.size() > 1) {
      reagentsCompartments = KBaseUtils.decideCompartment(reagentsCompartments);
    }
    if (reagentsCompartments.size() == 1) {
      rxnCmp = reagentsCompartments.iterator().next();
      rxnCmpSymbol = rxnCmp.substring(0, 1);
      rxnCmpRef = String.format("~/modelcompartments/id/%s", rxnCmp);
    } else {
      logger.warn("{} {}", rxnEntry, reagentsCompartments);
    }

    String rxnRef = "rxn00000";

    if (this.rxnToModelSeedReference.containsKey(rxnEntry)) {
      rxnRef = rxnToModelSeedReference.get(rxnEntry);
    }

    String rxnId = rxnEntry;
    if (this.removeReactionPrefix != null && 
        rxnEntry.contains(this.removeReactionPrefix)) {
      rxnId = rxnEntry.substring(this.removeReactionPrefix.length());
    }

    String[] boundStr = validateReactionContraint(rxnEntry, xmodel, xrxn, xrxn.getListOfParameters());

    String direction = "=";
    double lb = -1000000;
    double ub =  1000000;
    try {
      lb = Double.parseDouble(boundStr[0]);
    } catch (Exception e) { }
    try {
      ub = Double.parseDouble(boundStr[1]);
    } catch (Exception e) { }

    if (lb < 0 && ub > 0) {

    } else if (ub > 0 && lb >= 0) {
      direction = ">";
    } else if (lb < 0 && ub <= 0) {
      direction = "<";
    }
    //    System.out.println(new Range(lb, ub) + " " + direction);
    
    ModelReaction krxn = new ModelReaction().withId(rxnId)
        .withAliases(new ArrayList<String> ())
        .withName(rxnName)
        .withImportedGpr(gpr)
        .withDirection(direction)
        .withProtons(0.0)
        .withReactionRef("~/template/reactions/id/" + rxnRef + "_" + rxnCmpSymbol)
        .withModelReactionProteins(new ArrayList<ModelReactionProtein> ())
        .withProbability(0.0)
        .withPathway(null)
        .withDblinks(dblinks)
        .withMaxrevflux(Math.abs(lb))
        .withMaxforflux(Math.abs(ub))
        .withModelReactionReagents(reagents)
        .withStringAttributes(extraAttributes)
        .withModelcompartmentRef(rxnCmpRef);
    return krxn;
  }

  public FBAModel build() {
//    @SuppressWarnings("deprecation")
    XmlSbmlModelAdapter xadapter = new XmlSbmlModelAdapter(xmodel);
    
    FBAModel model = new FBAModel();
    model.setId(modelId);
    model.setName(modelName); //get from xml if exists
    model.setGenomeRef(KBaseConfig.REF_EMPTY_GENOME);
    if (genomeRef != null) {
      model.setGenomeRef(genomeRef);
    }
    model.setSource("External");
    model.setSourceId(modelId);
    model.setType("SBML Model");
    model.setTemplateRef(KBaseConfig.REF_TEMPLATE_G_NEG);
    model.setGapfillings(new ArrayList<ModelGapfill> ());
    model.setGapgens(new ArrayList<ModelGapgen> ());
    model.setBiomasses(new ArrayList<Biomass> ());
    model.setModelcompounds(new ArrayList<> (modelCompounds.values()));
    model.setModelcompartments(modelCompartments);
    model.setModelreactions(new ArrayList<ModelReaction> ());
    
    int biomassCounter = 1;
    
    for (XmlSbmlReaction xrxn : xmodel.getReactions()) {
      logger.trace("{}", xrxn.getAttributes());
      
      String rxnEntry = xrxn.getAttributes().get("id");
      ModelReaction krxn = buildReaction(xrxn, xadapter);
      
      if (biomassSet.contains(rxnEntry)) {
        Biomass biomass = KBaseModelAdapter.modelReactionToBiomass(krxn);
        biomass.setId("bio" + biomassCounter++);
        model.getBiomasses().add(biomass);
      } else {
        model.getModelreactions().add(krxn);
      }
    }
    
//    System.out.println(this.modelCompounds.keySet());
//    System.out.println(xadapter.xspiType);
    for (String spi : xadapter.xspiType.keySet()) {
//      System.out.println(spi);
      EntityType etype = xadapter.xspiType.get(spi);
      if (EntityType.GENE.equals(etype)) {
        ModelCompound mc = this.modelCompounds.get(spi);
        if (mc != null) {
          mc.getAdditionalProperties().put("type", "GENE");
        } else {
          logger.info("specie [{}] deleted", spi);
        }
      }
    }
    
    return model;
  }
  

  


  /**
   * Determine reaction reversibility
   */
  public static String[] validateReactionContraint(String rxnEntry, XmlSbmlModel xmodel, XmlSbmlReaction xrxn, List<XmlObject> rxnParameters) {
    //    System.out.println(rxnAttributes);

    String lb = null;
    String ub = null;

    //    System.out.println(xrxn.getAttributes());

    for (XmlObject xo : rxnParameters) {
      //      System.out.println(xo.getAttributes());
      String id = xo.getAttributes().get("id");
      if (id == null) {
        //        this.messages.add(new XmlMessage(xo, MessageType.WARN, "parameter without ID (rxn: %s)", rxnEntry));
        logger.trace("parameter without ID {}", xo.getAttributes());
      } else {
        String value = xo.getAttributes().get("value");
        //      String units = xo.getAttributes().get("units");
        //      System.out.println("\t\t:\t" + xo.getAttributes());
        switch (id.toUpperCase()) {
        case "OBJECTIVE_COEFFICIENT":
          //          this.defaultObjective.put(rxnEntry, value);
          break;
        case "LOWER_BOUND":
          lb = value;
          break;
        case "UPPER_BOUND":
          ub = value;
          break;
        default:
          //          CollectionUtils.increaseCount(ignoredParameter, id, 1);
          logger.trace("ignored {}", id);
          break;
        }
      }
    }

    if (xrxn.getAttributes().containsKey("upperFluxBound")) {
      String ufbcId = xrxn.getAttributes().get("upperFluxBound");
      for (XmlObject xo : xmodel.getListOfParameters()) {
        String id = xo.getAttributes().get("id");
        if (id != null && id.equals(ufbcId)) {
          String valueStr = xo.getAttributes().get("value");
          if (ub != null && !ub.equals(valueStr)) {
            logger.warn("fbc conflict with reaction parameters [{}] -> [{}]", valueStr, ub);
          } else {
            ub = valueStr;
          }
        }
      }
    }

    if (xrxn.getAttributes().containsKey("lowerFluxBound")) {
      String ufbcId = xrxn.getAttributes().get("lowerFluxBound");
      for (XmlObject xo : xmodel.getListOfParameters()) {
        String id = xo.getAttributes().get("id");
        if (id != null && id.equals(ufbcId)) {
          String valueStr = xo.getAttributes().get("value");
          if (lb != null && !lb.equals(valueStr)) {
            logger.warn("fbc conflict with reaction parameters [{}] -> [{}]", valueStr, lb);
          } else {
            lb = valueStr;
          }
        }
      }
    }

    Orientation parametersOrientation = Orientation.Unknown;
    Orientation attributesOrientation = Orientation.Unknown;
    //    if (lb == null || ub == null) {
    ////      logger.warn("{}, {}", lb, ub);
    //    } else {
    //      if (NumberUtils.isNumber(ub.trim()) && NumberUtils.isNumber(lb.trim())) {
    //        double ub_ = Double.parseDouble(ub.trim());
    //        double lb_ = Double.parseDouble(lb.trim());
    //        if (ub_ > 0.0 && lb_ < 0.0) {
    //          parametersOrientation = Orientation.Reversible;
    //        } else if (lb_ == 0.0 && ub_ > 0.0){
    //          parametersOrientation = Orientation.LeftToRight;
    //        } else if (lb_ < 0.0 && ub_ == 0.0) {
    //          parametersOrientation = Orientation.RightToLeft;
    //        } else if (lb_ == 0.0 && ub_ == 0.0) {
    //          logger.debug("zero [{}, {}]", lb, ub);
    //          parametersOrientation = Orientation.Zero;
    //        } else if (lb_ == ub_) {
    //          logger.debug("fixed bounds [{}, {}]", lb, ub);
    //          parametersOrientation = Orientation.Fixed;
    //        } else {
    //          parametersOrientation = Orientation.Range;
    //          //logger.error("strange bounds [{}, {}]", lb, ub);
    //        }
    //
    //      } else {
    //        logger.error("non numeric bounds [{}, {}]", lb, ub);
    //      }
    //    }

    String rev = xrxn.getAttributes().get("reversible");
    if (rev != null) {
      boolean reversible = Boolean.parseBoolean(rev);
      if (reversible) {
        attributesOrientation = Orientation.Reversible;
      } else {
        attributesOrientation = Orientation.LeftToRight;
      }
    }


    //    logger.debug("{} - type:{}, Attribute: {}, Parameters: {}", rxnEntry, xrxnTypeMap.get(rxnEntry), attributesOrientation, parametersOrientation);
    //
    //    //drains are special case we need to have lower and upper bounds !
    //    if (xrxnTypeMap.get(rxnEntry).equals(EntityType.DRAIN)) {
    //      if (lb == null || ub == null) {
    //        logger.debug("drain reaction with undefined bounds [{}, {}] !", lb, ub);
    //        this.messages.add(new XmlMessage(xrxn, MessageType.WARN, "%s drain reaction with undefined bounds [%s, %s] !", rxnEntry, lb, ub));
    //      }
    //      
    //      //if drain assume parameters or attributes (its up to the user to define uptakes)
    //      if (!parametersOrientation.equals(Orientation.Unknown)) {
    //        this.rxnDirection.get(parametersOrientation).add(rxnEntry);
    //      } else {
    //        this.rxnDirection.get(attributesOrientation).add(rxnEntry);
    //      }
    //    } else if (parametersOrientation.equals(Orientation.Unknown) && attributesOrientation.equals(Orientation.Unknown)) {
    //      this.rxnDirection.get(Orientation.Unknown).add(rxnEntry);
    //      this.messages.add(new XmlMessage(xrxn, MessageType.WARN, "no reversiblity information assume reversible (reversible:%s, #parameters %d)", rev, rxnParameters.size()));
    //      logger.debug("unable to determine orientation reversible:{}, #parameters {}", rev, rxnParameters.size());
    //    } else if (parametersOrientation.equals(attributesOrientation)) {
    //      logger.debug("orientation match ! {}", attributesOrientation);
    //      this.rxnDirection.get(attributesOrientation).add(rxnEntry);
    //    } else if (parametersOrientation.equals(Orientation.Unknown) && 
    //        !attributesOrientation.equals(Orientation.Unknown)) {
    //      logger.debug("using attribute orientation reversible:{} -> {}, #parameters {}", rev, attributesOrientation, rxnParameters.size());
    //      this.rxnDirection.get(attributesOrientation).add(rxnEntry);
    //    } else if (!parametersOrientation.equals(Orientation.Unknown) && 
    //        attributesOrientation.equals(Orientation.Unknown)) {
    //      logger.debug("using parameter orientation [{}, {}] -> {}, reversible:{}", lb, ub, parametersOrientation, rev);
    //      this.rxnDirection.get(parametersOrientation).add(rxnEntry);
    //    } else {
    //      this.messages.add(new XmlMessage(xrxn, MessageType.ERROR, "invalid orientation [%s, %s] -> %s", lb, ub, xrxnTypeMap.get(rxnEntry)));
    //      logger.debug("invalid orientation {}, [{}, {}] -> {}", xrxn.getAttributes(), lb, ub, xrxnTypeMap.get(rxnEntry));
    //      this.rxnDirection.get(Orientation.Unknown).add(rxnEntry);
    //    }
    return new String[]{lb, ub};
  }

  public static enum Orientation {
    LeftToRight, RightToLeft, Reversible, Unknown, 
    Fixed, //fixed constant non zero 
    Zero, //zero constant
    Range, //zero not included
  }
}
