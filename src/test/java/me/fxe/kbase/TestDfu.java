package me.fxe.kbase;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import datafileutil.DownloadStagingFileParams;
import us.kbase.common.service.JsonClientException;

public class TestDfu {

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
    try {
      KBaseAPI api = new KBaseAPI("244VEWPIFSAO6FGDRPTV5PYCQP5SYOZL", KBaseAPI.getConfigProd(), false);
      try {
//        api.dfuClient.
        api.dfuClient.downloadStagingFile(
            new DownloadStagingFileParams()
            .withStagingFileSubdirPath(""));
      } catch (JsonClientException e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    fail("Not yet implemented");
  }

}
