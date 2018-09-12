package me.fxe.kbase;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import kbasebiochem.Media;
import kbasebiochem.MediaCompound;
import kbasefba.FBAModel;
import kbasefba.ModelCompound;
import kbasefba.ModelReaction;
import kbasefba.ModelReactionProtein;
import kbasefba.ModelReactionReagent;
import pt.uminho.sysbio.biosynthframework.SimpleModelSpecie;
import pt.uminho.sysbio.biosynthframework.util.BiosIOUtils;

public class FixModelManual {
  
  private KBaseAPI api;
  private final String TOKEN = "2ND6UI5S5ITGP4JWIJGAPR6KVOMXPDYI";
  private final String WS = "jplfaria:narrative_1531842945859";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    api = new KBaseAPI(TOKEN, KBaseAPI.getConfigProd(), true);
  }

  @After
  public void tearDown() throws Exception {
  }
  
  public static ModelReactionReagent buildReagent(String o, double stoichiometry) {
//    String species = o.getAttributes().get("species");
//    ModelCompound kspi = modelCompounds.get(species);
//    String stoich = o.getAttributes().get("stoichiometry");
//    if (stoich == null) {
//      stoich = "1";
//    }

    ModelReactionReagent r = new ModelReactionReagent()
        .withCoefficient(stoichiometry)
        .withModelcompoundRef(String.format("~/modelcompounds/id/%s", o));
    return r;
  }
  
  public static ModelCompound export(FBAModel kmodel, KBaseModelAdapter adapter, ModelCompound cpd) {
  ModelCompound added = null;
    System.out.println(cpd);
    String spiEntry = KBaseModelAdapter.getEntryFromRef(cpd.getCompoundRef());
    System.out.println("export " + cpd.getId() + " " + spiEntry);
//    SimpleModelSpecie<String> s = adapter.getSpecies(spiEntry + "_e0");
    ModelCompound exportCompound = null;
    for (ModelCompound c : kmodel.getModelcompounds()) {
      if (c.getId().equals(spiEntry + "_e0")) {
        exportCompound = c;
        break;
      }
    }
    if (exportCompound == null) {
      System.out.println("create e0" + spiEntry);
      exportCompound = new ModelCompound()
          .withId(spiEntry + "_e0")
          .withModelcompartmentRef("~/modelcompartments/id/e0")
          .withDblinks(cpd.getDblinks())
          .withCompoundRef(cpd.getCompoundRef())
          .withFormula(cpd.getFormula())
          .withCharge(cpd.getCharge())
          .withName(cpd.getName());
      added = exportCompound;
    }
    List<ModelReactionReagent> reagents = new ArrayList<> ();
    ModelReactionReagent a = buildReagent(cpd.getId(), -1);
    ModelReactionReagent b = buildReagent(exportCompound.getId(), -1);
    reagents.add(a);
    reagents.add(b);
    ModelReaction krxn = new ModelReaction().withId("EXPORT_" + spiEntry)
        .withAliases(new ArrayList<String> ())
        .withName("Export " + spiEntry)
        .withImportedGpr("")
        .withDirection("=")
        .withProtons(0.0)
        .withReactionRef("~/template/reactions/id/rxn00000_e0")
        .withModelReactionProteins(new ArrayList<ModelReactionProtein> ())
        .withProbability(0.0)
        .withPathway(null)
        .withDblinks(new HashMap<String, List<String>>())
        .withMaxrevflux(Math.abs(1000.0))
        .withMaxforflux(Math.abs(1000.0))
        .withModelReactionReagents(reagents)
        .withStringAttributes(new HashMap<String, String>())
        .withModelcompartmentRef("~/modelcompartments/id/e0");
    kmodel.getModelreactions().add(krxn);
    
    return added;
  }
  
  public static void printE(FBAModel kmodel) {
    for (ModelCompound cpd : kmodel.getModelcompounds()) {
      if ("~/modelcompartments/id/e0".equals(cpd.getModelcompartmentRef())) {
        String cpdEntry = KBaseModelAdapter.getEntryFromRef(cpd.getCompoundRef());
        System.out.println(cpdEntry + "\t" + cpd.getId() + "\t" + cpd.getModelcompartmentRef() + " " + cpd.getName());    
      }
    }
  }

  @Test
  public void test() {
    //M_5drib_c cpd15380_c0
    //M_4crsol_c cpd01042_c0
    //M_amob_c  cpd02701_c0
    Set<String> export = new HashSet<>();
    export.add("cpd15380");
    export.add("cpd01042");
    export.add("cpd02701");
    export.add("cpd00116");
    
    try {
      FBAModel kmodel = api.getModel("iML1515_kbase_integration", WS);
      Object o = api.getWorkspaceObject("Carbon-D-Glucose", WS);
      ObjectMapper om = new ObjectMapper();
      Media media = om.convertValue(o, Media.class);
      KBaseModelAdapter adapter = new KBaseModelAdapter(kmodel);
      for (MediaCompound mc : media.getMediacompounds()) {
//        System.out.println(mc.getCompoundRef());
        System.out.println(mc);
      }
      media.getMediacompounds().add(
          new MediaCompound().withCompoundRef("489/6/5/compounds/id/cpd00011").
          withConcentration(0.001)
          .withMaxFlux(100.000)
          .withMinFlux(-100.00));
      media.getMediacompounds().add(
          new MediaCompound().withCompoundRef("489/6/5/compounds/id/cpd15380").
          withConcentration(0.001)
          .withMaxFlux(100.000)
          .withMinFlux(-100.00));
      media.getMediacompounds().add(
          new MediaCompound().withCompoundRef("489/6/5/compounds/id/cpd01042").
          withConcentration(0.001)
          .withMaxFlux(100.000)
          .withMinFlux(-100.00));
      media.getMediacompounds().add(
          new MediaCompound().withCompoundRef("489/6/5/compounds/id/cpd02701").
          withConcentration(0.001)
          .withMaxFlux(100.000)
          .withMinFlux(-100.00));
      media.getMediacompounds().add(
          new MediaCompound().withCompoundRef("489/6/5/compounds/id/cpd00116").
          withConcentration(0.001)
          .withMaxFlux(100.000)
          .withMinFlux(-100.00));
      api.save(media, "test_media", WS);
      
      Set<ModelCompound> added = new HashSet<>();
      for (ModelCompound cpd : kmodel.getModelcompounds()) {
        if ("~/modelcompartments/id/e0".equals(cpd.getModelcompartmentRef())) {
          if (cpd.getId().equals("cpd20863_e0")) {
            System.out.println(cpd);
            cpd.setCompoundRef("~/template/compounds/id/cpd00030");
            cpd.setId("cpd00030_e0");
            for (ModelReaction rxn : kmodel.getModelreactions()) {
              for (ModelReactionReagent rg : rxn.getModelReactionReagents()) {
                if (rg.getModelcompoundRef().equals("~/modelcompounds/id/cpd20863_e0")) {
                  System.out.println(rxn.getId() + " " + rg);
                  rg.setModelcompoundRef("~/modelcompounds/id/cpd00030_e0");
                }
              }
            }
          }
        } else {
          for (String s : export) {
            if (cpd.getId().contains(s) && "~/modelcompartments/id/c0".equals(cpd.getModelcompartmentRef())) {
              ModelCompound added_ = export(kmodel, adapter, cpd);
              if (added_ != null) {
                added.add(added_);
              }
            }
          }
          
//          System.out.println(cpd.getId());
        }
      }
      kmodel.getModelcompounds().addAll(added);
      
//      printE(kmodel);
      
//      api.save(kmodel, "test_model", WS);
    } catch (IOException e) {
      e.printStackTrace();
    }
    fail("Not yet implemented");
  }

}
