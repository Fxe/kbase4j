package me.fxe.kbase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.uminho.sysbio.biosynthframework.SubcellularCompartment;
import pt.uminho.sysbio.biosynthframework.util.ZipContainer;
import pt.uminho.sysbio.biosynthframework.util.ZipContainer.ZipRecord;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.Tuple11;
import us.kbase.common.service.UObject;
import us.kbase.workspace.GetObjects2Params;
import us.kbase.workspace.GetObjects2Results;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectSpecification;
import us.kbase.workspace.WorkspaceClient;

public class KBaseUtils {

  private static final Logger logger = LoggerFactory.getLogger(KBaseUtils.class);

  public static String getRefFromObjectInfo(Tuple11<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> info) {
    if (info != null) {
      return info.getE7() + "/" + info.getE1() + "/" + info.getE5();
    }

    return null;
  }

  public static Object getObject(String name, String ws, String ref, WorkspaceClient wsClient) throws IOException {
    try {
      List<ObjectSpecification> objects = new ArrayList<> ();
      ObjectSpecification ospec = new ObjectSpecification();
      if (name != null) {
        ospec.withName(name);
      }
      if (ws != null) {
        ospec.withWorkspace(ws);
      }
      if (ref != null) {
        ospec.withRef(ref);
      }

      objects.add(ospec);
      GetObjects2Params params = new GetObjects2Params().withObjects(objects);
      GetObjects2Results result = wsClient.getObjects2(params);
      List<ObjectData> odatas = result.getData();
      ObjectData odata = odatas.iterator().next();
      ref = getRefFromObjectInfo(odata.getInfo());

      Object o = odata.getData().asInstance();
      return o;
    } catch (IOException | JsonClientException e) {
      throw new IOException(e);
    }
  }

  public static<T> T getObject(String name, String ws, String ref, Class<T> clazz,
      WorkspaceClient wsClient) throws IOException {
    Object o = getObject(name, ws, ref, wsClient);
    ObjectMapper om = new ObjectMapper();
    T fbaModel = om.convertValue(o, clazz);
    return fbaModel;
  }

  public static String save(String nameId, String dataType, Object o, String ws, final WorkspaceClient wsClient) throws Exception {
//    long wsId = dfuClient.wsNameToId(ws);

//    SaveObjectsParams params = new SaveObjectsParams()
//        .withId(wsId)
//        .withObjects(Arrays.asList(
//            new ObjectSaveData().withName(nameId)
//            .withType(dataType)
//            .withData(new UObject(o))));
    ////  params.setId(wsId);
    ////  List<ObjectSaveData> saveData = new ArrayList<> ();
    ////  ObjectSaveData odata = new ObjectSaveData();
    ////  odata.set
    ////  
    ////  params.setObjects(saveData);
    ////  ;
    List<us.kbase.workspace.ObjectSaveData>objects = new ArrayList<> ();
    objects.add(new us.kbase.workspace.ObjectSaveData().withData(
        new UObject(o)).withName(nameId).withType(dataType));
//    WorkspaceIdentity wsi = new WorkspaceIdentity().withWorkspace(ws);
//    long wsId = wsClient.getWorkspaceInfo(wsi).getE1();
    us.kbase.workspace.SaveObjectsParams params = new us.kbase.workspace.SaveObjectsParams()
//        .withId(wsId)
        .withWorkspace(ws)
        .withObjects(objects);
    List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> mg = 
        wsClient.saveObjects(params);
//    KBaseId result = null;
//    for (Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>> info : mg) {
//      String ref = KBaseIOUtils.getRefFromObjectInfo(info);
//      result = new KBaseId(info.getE2(), info.getE8(), ref);
//    }
    String ref = getRefFromObjectInfo(mg.get(0));

    return ref;
  }
  
  public static byte[] loadJsonFromZip(String path) throws IOException {
    byte[] bytes = new byte[0];
    File f = new File(path);
    if (!f.exists()) {
      throw new FileNotFoundException(path);
    }
    if (!f.isFile() || !f.getName().endsWith(".zip")) {
      throw new IOException("Bad path (must be a file ending with .zip): " + path);
    }

    String lookup = f.getName().substring(0, f.getName().length() - 4);
    ZipContainer zipContainer = new ZipContainer(path);
    InputStream is = null;
    for (ZipRecord zr : zipContainer.getInputStreams()) {
      if (zr.name.equals(lookup)) {
        is = zr.is;
      }
    }

    if (is != null) {
      bytes = IOUtils.toByteArray(is);
      logger.debug("read {} KBs", bytes.length / 1024.0);
    } else {
      zipContainer.close();
      throw new IOException("Bad zip file. Genome data not found. Expected: " + lookup);
    }

    zipContainer.close();

    return bytes;
  }
  
  public static String toSymbol(SubcellularCompartment scmp) {
    if (scmp == null) {
      return "z";
    }
    switch (scmp) {
      case BOUNDARY: return "b";
      case EXTRACELLULAR: return "e";
      case CYTOSOL: return "c";
      case PERIPLASM: return "p";
      case MITOCHONDRIA: return "m";
      case NUCLEUS: return "n";
      case VACUOLE: return "v";
      case GOLGI: return "g";
      case RETICULUM: return "r";
      case MITOCHONDRIA_MEMBRANE: return "j";
      case PEROXISOME: return "x";
      case CARBOXYSOME: return "a";
      case LYSOSOME: return "l";
//      case PLASTID: return "d";
//      case CELL_WALL: return "w";
      default: return "z";
    }
  }
  
  public static<T> Set<T> singleton(T e) {
    Set<T> set = new HashSet<>();
    set.add(e);
    return set;
  }

  public static String getEntryFromRef(String ref) {
    String[] tks = ref.split("/");
    String s = tks[tks.length - 1];
    return s;
  }
  
  public static Set<String> decideCompartment(Set<String> cmps) {
    if (cmps.size() == 2) {
      Iterator<String> it = cmps.iterator();
      String c1 = it.next();
      String c2 = it.next();
      String s1 = c1.substring(0, 1);
      String s2 = c2.substring(0, 1);
      Map<String, String> s = new HashMap<>();
      s.put(s1, c1);
      s.put(s2, c2);
      if (s.containsKey("e") && s.containsKey("c")) {
        return singleton(s.get("c"));
      } else if (s.containsKey("e") && s.containsKey("c")) {
        return singleton(s.get("c"));
      } else if (s.containsKey("m") && s.containsKey("c")) {
        return singleton(s.get("m"));
      } else if (s.containsKey("d") && s.containsKey("c")) {
        return singleton(s.get("d"));
      } else if (s.containsKey("x") && s.containsKey("c")) {
        return singleton(s.get("x"));
      } else if (s.containsKey("v") && s.containsKey("c")) {
        return singleton(s.get("v"));
      } else if (s.containsKey("e") && s.containsKey("d")) {
        return singleton(s.get("d"));
      } else if (s.containsKey("m") && s.containsKey("j")) {
        return singleton(s.get("j"));
      } else if (s.containsKey("c") && s.containsKey("p")) {
        return singleton(s.get("p"));
      } else if (s.containsKey("p") && s.containsKey("e")) {
        return singleton(s.get("e"));
      }
      
      //System.out.println(s1 + " " + s2);
    } else {
      return new HashSet<>(cmps);
    }
    return new HashSet<>(cmps);
  }
}
