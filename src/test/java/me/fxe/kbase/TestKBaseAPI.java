package me.fxe.kbase;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import kbasegenomes.Feature;
import kbasegenomes.Genome;
import pt.uminho.sysbio.biosynthframework.util.BiosIOUtils;

public class TestKBaseAPI {

  private KBaseAPI api;
  private final String TOKEN = "ME2Q4GNKLF5TOCNJ54XNTKRUWI6EODHG";
  private final String TEST_WS = "filipeliu:1452618747692";
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    String s = null;
    try (InputStream is =  new FileInputStream("D:\\tmp\\integration/GCF_000005845.2.json")) {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      IOUtils.copy(is, os);
      s = os.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    BiosIOUtils.fromJson(s, Genome.class);
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

  @Test
  public void testGetMedia() throws IOException {
    Object o = api.getWorkspaceObject("media", TEST_WS);
    assertNotNull(o);
    System.out.println(o);
  }
  
  @Test
  public void testGetGenome() throws IOException {
    Genome kgenome = api.getGenome("ecoli", TEST_WS);
    assertNotNull(kgenome);
    for (Feature feature : kgenome.getFeatures()) {
      System.out.println(feature.getId() + " " + feature.getAliases());
    }
  }
  
  @Test
  public void testListWorkspaceGenomes() throws IOException {
    List<String> os = api.listNarrative(TEST_WS, KBaseType.Genome);
    assertNotNull(os);
    System.out.println(os);
  }
  
  @Test
  public void testSaveGenomeToWorkspace() throws IOException {
    Genome kgenome = api.getGenome("ecoli", TEST_WS);
    api.save(kgenome, "ecoli_SAVED", TEST_WS);
    List<String> os = api.listNarrative(TEST_WS, KBaseType.Genome);
    assertNotNull(os);
    System.out.println(os);
  }
}
