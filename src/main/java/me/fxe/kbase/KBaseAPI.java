package me.fxe.kbase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import genomeannotationapi.GenomeAnnotationApiClient;
import genomeannotationapi.GenomeDataSetV1;
import genomeannotationapi.GenomeDataV1;
import genomeannotationapi.GenomeSelectorV1;
import genomeannotationapi.GetGenomeParamsV1;
import genomefileutil.GenomeFileUtilClient;
import genomefileutil.SaveOneGenomeParams;
import kbasebiochem.Media;
import kbasefba.FBAModel;
import kbasegenomes.Genome;
import me.fxe.kbase.CacheEngine.CacheFieldType;
import pt.uminho.sysbio.biosynthframework.Tuple2;
import us.kbase.auth.AuthConfig;
import us.kbase.auth.AuthException;
import us.kbase.auth.AuthToken;
import us.kbase.auth.ConfigurableAuthService;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.Tuple11;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.workspace.CopyObjectParams;
import us.kbase.workspace.GetObjectInfo3Params;
import us.kbase.workspace.GetObjectInfo3Results;
import us.kbase.workspace.ListObjectsParams;
import us.kbase.workspace.ObjectIdentity;
import us.kbase.workspace.ObjectSpecification;
import us.kbase.workspace.WorkspaceClient;

public class KBaseAPI {
  
  public static String REFERENCE_GENOMES = "ReferenceDataManager";
  
  public final String token;
  public AuthToken authToken;
  public URL callbackURL;
  
  public WorkspaceClient wsClient;
  public GenomeFileUtilClient gfuClient;
  public GenomeAnnotationApiClient gaClient;
  
  public static Map<String, Map<Pair<String, String>, Map<CacheFieldType, String>>> cacheIndexMap = new HashMap<> ();
  
  public static Map<String, String> getConfigProd() {
    Map<String, String> config = new HashMap<> ();
    if (!config.containsKey("kbase-endpoint")) {
      config.put("kbase-endpoint", "https://kbase.us/services");
    }
    if (!config.containsKey("job-service-url")) {
      config.put("job-service-url", "https://kbase.us/services/userandjobstate/");
    }
    config.put("callback-url", "https://kbase.us/services/njs_wrapper");
    config.put("workspace-url", "https://kbase.us/services/ws/");
    config.put("shock-url", "https://kbase.us/services/shock-api");
    config.put("auth-service-url", "https://kbase.us/services/auth/api/legacy/KBase/Sessions/Login");
    config.put("auth-service-url-allow-insecure", "false");
    config.put("version", "prod");
    
    return config;
  }
  
  public static Map<String, String> getConfigDev() {
    Map<String, String> config = new HashMap<> ();
    if (!config.containsKey("kbase-endpoint")) {
      config.put("kbase-endpoint", "https://appdev.kbase.us/services");
    }
    if (!config.containsKey("job-service-url")) {
      config.put("job-service-url", "https://appdev.kbase.us/services/userandjobstate/");
    }
    config.put("callback-url", "https://appdev.kbase.us/services/njs_wrapper");
    config.put("workspace-url", "https://appdev.kbase.us/services/ws/");
    config.put("shock-url", "https://appdev.kbase.us/services/shock-api");
    config.put("auth-service-url", "https://appdev.kbase.us/services/auth/api/legacy/KBase/Sessions/Login");
    config.put("auth-service-url-allow-insecure", "false");
    config.put("version", "appdev");
    return config;
  }
  
  public String getIdFromReference(String ref) {
    Set<String> ids = new HashSet<>();
    List<Tuple11<Long,String,String,String,Long,String,Long,String,String,Long,Map<String,String>>> data = null;
    ObjectIdentity oi = new ObjectIdentity().withRef(ref);
    try {
      data = wsClient.getObjectHistory(oi);
      for (Tuple11<?, String, ?, ?, ?, ?, ?, ?, ?, ?, ?> o : data) {
        ids.add(o.getE2());
      }
    } catch (IOException | JsonClientException e) {
      e.printStackTrace();
    }
    if (ids.size() == 1) {
      return ids.iterator().next();
    }
    return null;
  }
  
  public KBaseAPI(final String token, final Map<String, String> config, final boolean cache) throws IOException {
    this.token = token;
    String authUrl = config.get("auth-service-url");
    
    try {
      ConfigurableAuthService authService =  new ConfigurableAuthService(
          new AuthConfig().withKBaseAuthServerURL(new URL(authUrl))
          .withAllowInsecureURLs("true".equals(config.get("auth-service-url-allow-insecure"))));
      authToken = authService.validateToken(token);
      
      callbackURL = new URL(config.get("callback-url"));
      if (cache) {
        CacheEngine cengine = new CacheEngine(cacheIndexMap, config.get("version"));
        CacheEngine.buildCacheIndex(CacheEngine.cacheIndexFile, cacheIndexMap);
        cengine.status();
        wsClient = new CacheWorkspaceClient(new URL(config.get("workspace-url")), authToken, cengine);
//        solrClient = new MockKBSolrUtilClient(callbackURL, authToken);
      } else {
        wsClient = new WorkspaceClient(new URL(config.get("workspace-url")), authToken);
        
//        solrClient = new KBSolrUtilClient(callbackURL, authToken);
      }
      
      gaClient = new GenomeAnnotationApiClient(callbackURL, authToken);
      gfuClient = new GenomeFileUtilClient(callbackURL, authToken);
//      gaClient = new GenomeAnnotationAPIClient(callbackURL, authToken);
//      gaClient.setConnectionReadTimeOut(900000000);
//      gaClient.setAsyncJobCheckMaxTimeMs(6000000);
//      dfuClient = new DataFileUtilClient(callbackURL, authToken);
//      dfuClient.setIsInsecureHttpConnectionAllowed(false);
//      gaClient.setIsInsecureHttpConnectionAllowed(false);
      wsClient.setIsInsecureHttpConnectionAllowed(false);
//      solrClient.setIsInsecureHttpConnectionAllowed(false);
//      solrClient.setServiceVersion("beta");
    } catch (IOException | URISyntaxException | AuthException | UnauthorizedException e) {
      authToken = null; 
      throw new IOException(e);
    }
  }
  
  public WorkspaceClient getWorkspaceClient() {
    return wsClient;
  }
  
  public Object getWorkspaceObject(String id, String ws) throws IOException {
    Object o = KBaseUtils.getObject(id, ws, null, getWorkspaceClient());
    return o;
  }
  
  public<T> T getWorkspaceObject(String id, String ws, Class<T> clazz) throws IOException {
    Object o = KBaseUtils.getObject(id, ws, null, getWorkspaceClient());
    ObjectMapper om = new ObjectMapper();
    return om.convertValue(o, clazz);
  }
  
  public Genome getGenome(String id, String ws) throws IOException {
    Genome o = KBaseUtils.getObject(id, ws, null, Genome.class, getWorkspaceClient());
    return o;
  }
  

  public Genome getGenome2(String ref) {
    GenomeSelectorV1 gs = new GenomeSelectorV1().withRef(ref);
    List<GenomeSelectorV1> selectors = new ArrayList<>();
    selectors.add(gs);
    GetGenomeParamsV1 params = new GetGenomeParamsV1().withDowngrade(0L)
                                                      .withGenomes(selectors);
    try {
      GenomeDataSetV1 result = gaClient.getGenomeV1(params);
      for (GenomeDataV1 data : result.getGenomes()) {
        System.out.println(data.getInfo());
      }
    } catch (IOException | JsonClientException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public FBAModel getModel(String id, String ws) throws IOException {
    FBAModel o = KBaseUtils.getObject(id, ws, null, FBAModel.class, getWorkspaceClient());
    return o;
  }
  
  public Tuple2<String> getInfoFromRef(String ref) {
    String id = null;
    String ws = null;
    List<ObjectSpecification> aa = new ArrayList<> ();
    aa.add(new ObjectSpecification().withRef(ref));
    GetObjectInfo3Params params = new GetObjectInfo3Params().withObjects(aa);
    try {
      GetObjectInfo3Results result = wsClient.getObjectInfo3(params);
      id = result.getInfos().iterator().next().getE2();
      ws = result.getInfos().iterator().next().getE8();
    } catch (IOException | JsonClientException e) {
      e.printStackTrace();
    }
    
    return new Tuple2<String>(id, ws);
  }
  
  public List<String> listNarrative(String ws, String otype) throws IOException {
    List<String> result = new ArrayList<> ();
    List<String> workspaces = new ArrayList<> ();
    workspaces.add(ws);
    try {
      ListObjectsParams lparams = new ListObjectsParams().withWorkspaces(workspaces);
      if (!"Any".equals(otype)) {
        lparams.withType(otype);
      }
      List<Tuple11<Long,String,String,String,Long,String,Long,String,String,Long,Map<String,String>>> o = 
      wsClient.listObjects(lparams);
      for (Tuple11<Long,String,String,String,Long,String,Long,String,String,Long,Map<String,String>> t : o) {
//        String oref = String.format("%d/%d/%d", t.getE7(), t.getE1(), t.getE5());
        String oname = t.getE2();
//        String ows = t.getE8();
        result.add(oname);
      }
    } catch (IOException | JsonClientException e) {
      throw new IOException(e);
    }
    
    return result;
  }
  
  public List<String> listNarrative(String ws, KBaseType otype) throws IOException {
    return listNarrative(ws, otype.value());
  }
  
  public List<String> listNarrative(String ws) throws IOException {
    return listNarrative(ws, KBaseType.Any.value());
  }

  public void save(FBAModel model, String name, String ws) {
    try {
      KBaseUtils.save(name, KBaseType.FBAModel.value(), model, ws, wsClient);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void save(Media media, String name, String ws) {
    try {
      KBaseUtils.save(name, KBaseType.KBaseBiochemMedia.value(), media, ws, wsClient);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void save(Genome genome, String name, String ws) {
    SaveOneGenomeParams params = new SaveOneGenomeParams().withData(genome)
                                                          .withName(name)
                                                          .withWorkspace(ws);
    try {
      gfuClient.saveOneGenome(params);
    } catch (IOException | JsonClientException e) {
      e.printStackTrace();
    }
  }
  
  public void save(Object o, String name, String ws, KBaseType type) {
    try {
      KBaseUtils.save(name, type.value(), o, ws, wsClient);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void copy(String id, String wsSrc, String wsDst) throws IOException {
    ObjectIdentity oiSrc = new ObjectIdentity().withWorkspace(wsSrc).withName(id);
    ObjectIdentity oiDst = new ObjectIdentity().withWorkspace(wsDst).withName(id);
    CopyObjectParams cop = new CopyObjectParams().withFrom(oiSrc).withTo(oiDst);
    try {
      this.wsClient.copyObject(cop);
    } catch (JsonClientException e) {
      throw new IOException(e);
    }
  }

}
