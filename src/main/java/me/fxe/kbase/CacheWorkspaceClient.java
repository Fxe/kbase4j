package me.fxe.kbase;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.print.attribute.standard.Media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kbasegenomes.Genome;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.RpcContext;
import us.kbase.common.service.UObject;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.workspace.GetObjects2Params;
import us.kbase.workspace.GetObjects2Results;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectSpecification;
import us.kbase.workspace.WorkspaceClient;

public class CacheWorkspaceClient extends WorkspaceClient {

  private static final Logger logger = LoggerFactory.getLogger(CacheWorkspaceClient.class);
  
  public final CacheEngine cacheEngine;
  public Map<String, Class<?>> otypeMap = new HashMap<> ();
  
  public CacheWorkspaceClient(URL url, AuthToken token, CacheEngine cengine) throws IOException, UnauthorizedException {
    super(url, token);
    this.cacheEngine = cengine;
    otypeMap.put("KBaseGenomes.Genome-8.2", Genome.class);
    otypeMap.put("KBaseGenomes.Genome-8.3", Genome.class);
//    otypeMap.put("KBaseFBA.FBAModel-9.1", FBAModel.class);
//    otypeMap.put("KBaseFBA.FBAModel-11.0", FBAModel.class);
    otypeMap.put("KBaseBiochem.Media-4.0", Media.class);
  }
  
  @Override
  public GetObjects2Results getObjects2(GetObjects2Params params, RpcContext... jsonRpcContext)
      throws IOException, JsonClientException {
    
    logger.trace("Params: {}", params);
    
    GetObjects2Results results = new GetObjects2Results();
    List<ObjectData> odata = new ArrayList<> ();
    results.withData(odata);
    for (ObjectSpecification ospec : params.getObjects()) {
      String id = ospec.getName();
      String ws = ospec.getWorkspace();
      if (id != null && ws != null) {
        if (cacheEngine.isCached(id, ws)) {
          logger.debug("found in cache {}, {}", id, ws);
          Object data = cacheEngine.getCacheData(id, ws);
          UObject uo = new UObject(data);
//          Tuple11<Long,String,String,String,Long,String,Long,String,String,Long,Map<String,String>> info = cacheEngine.getInfo(id, ws);
          odata.add(new ObjectData().withInfo(null).withData(uo));
        } else {
          logger.info("fetch live {}, {}", id, ws);
          List<ObjectSpecification> objects = new ArrayList<> ();
          objects.add(new ObjectSpecification().withName(id).withWorkspace(ws));
          GetObjects2Results result = super.getObjects2(new GetObjects2Params().withObjects(objects), jsonRpcContext);
          for (ObjectData o : result.getData()) {
            String ref = KBaseUtils.getRefFromObjectInfo(o.getInfo());
            cacheEngine.save(o.getInfo().getE2(), //id
                             o.getInfo().getE8(), //ws
                             ref, //ref
                             o.getInfo().getE3(), //type
                             o.getInfo().getE6(), //author
                             o.getData().toJsonString());
            odata.add(o);
          }
        }
      }
    }
    
//        super.getObjects2(params, jsonRpcContext);
    return results;
  }
}
