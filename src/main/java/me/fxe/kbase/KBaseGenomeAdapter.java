package me.fxe.kbase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;

import kbasegenomes.Feature;
import kbasegenomes.Genome;
import pt.uminho.sysbio.biosynthframework.util.DataUtils;

public class KBaseGenomeAdapter {
  
  private static final Logger logger = LoggerFactory.getLogger(KBaseGenomeAdapter.class);
  
  public final Genome genome;
  public Map<String, Set<Feature>> featureMap = new HashMap<> ();
  public Map<String, Feature> idToFeature = new HashMap<> ();
  public Map<String, String> aliasToFeatureId = new HashMap<> ();
  
  public static String getAlias(List<String> tuple) {
    if (tuple.size() >= 2) {
      return tuple.get(1);
    } else if (!tuple.isEmpty()) {
      return tuple.get(0);
    }
    return null;
  }
  
  public KBaseGenomeAdapter(Genome genome) {
    this.genome = genome;
    
    for (Feature f: genome.getFeatures()) {
      idToFeature.put(f.getId(), f);
      addFeature(f.getId(), f);
      for (List<String> l : f.getAliases()) {
        String a = getAlias(l);
        if (!DataUtils.empty(a)) {
          addFeature(a, f);
        }
      }
    }
  }
  
  public void addUniqueAliases(String id, List<String> aliases) {
    Set<Feature> features = featureMap.get(id);
    if (features.size() == 1) {
      Feature f = features.iterator().next();
      for (String a : aliases) {
        if (!featureMap.containsKey(a)) {
          addFeature(a, f);
          List<String> l = new ArrayList<> ();
          l.add("");
          l.add(a);
          f.getAliases().add(l);
        } else {
          logger.warn("duplicate alias: {}", a);
        }
      }
    } else {
      logger.warn("invalid feature ID: {}", id);
    }
//    System.out.println(features.size());
//    System.out.println(featureMap.get(id));
//    System.out.println(aliases);
  }
  
  private void addFeature(String alias, Feature f) {
    if (!featureMap.containsKey(alias)) {
      featureMap.put(alias, new HashSet<Feature>());
    }
    if (!aliasToFeatureId.containsKey(alias)) {
      aliasToFeatureId.put(alias, f.getId());
    }
    featureMap.get(alias).add(f);
  }
  
  public Feature findUniqueFeature(String alias) {
    Feature f = null;
    if (featureMap.containsKey(alias)) {
      if (featureMap.get(alias).size() == 1) {
        f = featureMap.get(alias).iterator().next();
      } else {
        throw new IllegalArgumentException(String.format("Alias not unique: [%s]", alias));
      }
    }
    
    return f;
  }
  
  public String findUniqueFeature(String alias, Predicate<String> filter) {
    Feature f = findUniqueFeature(alias);
    
    String id = null;
    
    if (f != null) {
      Set<String> aliases = new HashSet<>();
      for (List<String> tuple : f.getAliases()) {
        aliases.add(getAlias(tuple));
      }
      aliases.add(f.getId());
      for (String a : aliases) {
        if (filter.apply(a)) {
          id = a;
        }
      }
    }
    
    return id;
  }

  public String getFunction(String geneId) {
    Feature feature = this.idToFeature.get(geneId);
    Set<String> features = new TreeSet<>();
    String f = feature.getFunction();
    if (f != null) {
      features.add(f);
    }
    List<String> fs = feature.getFunctions();
    if (fs != null) {
      for (String fss : fs) {
        if (fss != null) {
          features.add(fss);
        }
      }
    }
    
    if (features.isEmpty()) {
      return null;
    }
    
    return Joiner.on(" ; ").join(features);
  }


  
//  public Feature findFeature(String alias) {
//    
//  }
}

