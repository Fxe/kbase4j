package me.fxe.kbase;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.uminho.sysbio.biosynthframework.Tuple2;
import pt.uminho.sysbio.biosynthframework.integration.model.XmlReferencesBaseIntegrationEngine;
import pt.uminho.sysbio.biosynthframework.sbml.SbmlNotesParser;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlModel;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlModelAdapter;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlReaction;
import pt.uminho.sysbio.biosynthframework.sbml.XmlSbmlSpecie;
import pt.uminho.sysbio.biosynthframework.sbml.XmlStreamSbmlReader;
import pt.uminho.sysbio.biosynthframework.util.DataUtils;

public class TestKBaseFBAModelFactory {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void test() {
//    try (InputStream is = new FileInputStream("/var/biomodels/sbml/iAF1260.xml")) {
//      
//    }R_TAURabcpp
    SbmlNotesParser notesParser = new SbmlNotesParser();
    XmlReferencesBaseIntegrationEngine engine = new XmlReferencesBaseIntegrationEngine(notesParser);
//    String mpath = "/var/biomodels/sbml/iAF1260.xml";
    String mpath = "/var/biomodels/sbml/Ecoli_K12_MG1655.xml";
    try {
      XmlStreamSbmlReader reader = new XmlStreamSbmlReader(new FileInputStream(mpath));
      XmlSbmlModel xmodel = reader.parse();
      XmlSbmlModelAdapter ada = new XmlSbmlModelAdapter(xmodel);
      for (XmlSbmlSpecie xspi : xmodel.getSpecies()) {
        String id = xspi.getAttributes().get("id");
        System.out.println(xspi.getAttributes());
        System.out.println(xspi.getListOfAnnotations());
        System.out.println(xspi.getAnnotation());
        System.out.println(xspi.getNotes());
        engine.detectMetadata(xspi);
//        System.out.println(ada.getGpr(rxnId));
//        System.out.println(xrxn.getAttributes());
//        String notes = xrxn.getNotes();
////        System.out.println(notes);
//        if (!DataUtils.empty(notes)) {
//          SbmlNotesParser parser = new SbmlNotesParser(notes);
//          parser.parse();
//          for (Tuple2<String> t : parser.getData()) {
//            System.out.println(t);
//          }
//        }

        
//        ada.getGpr(rxnId);
      }
//      for (String rxnId : ada.getReactionIds()) {
//        System.out.println(ada.getg);
//      }
//      FBAModel kmodel = new KBaseFBAModelFactory()
////          .withCompartmentMapping(cmap)
////          .withGenomeRef(genomeRef)
////          .withSpecieIntegration(sintegration)
////          .withReactionIntegration(rintegration)
////          .withBiomassIds(biomassIds)
////          .withMetaboliteModelSeedReference(spiToModelSeedReference)
////          .withReactionModelSeedReference(rxnToModelSeedReference)
//          .withModelId("iAF1260")
//          .withModelName("iAF1260")
//          .withXmlSbmlModel(xmodel, false)
//          .build();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
