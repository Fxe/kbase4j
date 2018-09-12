package pt.uminho.sysbio.biosynthframework.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kbasefba.Biomass;
import kbasefba.BiomassCompound;
import kbasefba.FBAModel;
import kbasefba.ModelCompound;
import kbasefba.ModelReaction;
import kbasefba.ModelReactionReagent;
import me.fxe.kbase.KBaseModelAdapter;
import pt.uminho.sysbio.biosynthframework.Range;

public class KBaseConstraintBasedModelFactory implements ConstraintBasedModelFactory {

  private static final Logger logger = LoggerFactory.getLogger(KBaseConstraintBasedModelFactory.class);
  
  private Map<String, Integer> spiIndexMap = new HashMap<>();
  private Map<String, Integer> rxnIndexMap = new HashMap<>();
  private FBAModel model;
  
  public KBaseConstraintBasedModelFactory(FBAModel model) {
    this.model = model;
    int spiIndex = 0;
    
    for (ModelCompound mc : model.getModelcompounds()) {
      spiIndexMap.put(mc.getId(), spiIndex++);
    }
    int rxnIndex = 0;
    for (ModelReaction mr : model.getModelreactions()) {
      rxnIndexMap.put(mr.getId(), rxnIndex++);
    }
    for (Biomass b : model.getBiomasses()) {
      rxnIndexMap.put(b.getId(), rxnIndex++);
    }
  }
  
  @Override
  public double[][] getMatrix() {
    double[][] matrix = new double[spiIndexMap.size()][rxnIndexMap.size()];

    for (ModelReaction mr : model.getModelreactions()) {
      Integer irxn = rxnIndexMap.get(mr.getId());
      for (ModelReactionReagent mrr : mr.getModelReactionReagents()) {
//        System.out.println(mrr.getModelcompoundRef());
        double value = mrr.getCoefficient();
        Integer ispi = spiIndexMap.get(KBaseModelAdapter.getEntryFromRef(mrr.getModelcompoundRef()));
//        System.out.println(irxn + " " + mrr.getModelcompoundRef() + " " + ispi);
        matrix[ispi][irxn] = value;
      }
    }
    for (Biomass b : model.getBiomasses()) {
      Integer irxn = rxnIndexMap.get(b.getId());
      for (BiomassCompound bc : b.getBiomasscompounds()) {
        double value = bc.getCoefficient();
        Integer ispi = spiIndexMap.get(KBaseModelAdapter.getEntryFromRef(bc.getModelcompoundRef()));
        matrix[ispi][irxn] = value;
      }
    }
    return matrix;
  }

  @Override
  public double[][] getBounds() {
    double[][] bounds = new double[rxnIndexMap.size()][2];
    for (ModelReaction mr : model.getModelreactions()) {
      Integer irxn = rxnIndexMap.get(mr.getId());
      double fflux = mr.getMaxforflux();
      double rflux = mr.getMaxrevflux();
      bounds[irxn][0] = -1 * rflux;
      bounds[irxn][1] =      fflux;
    }
    return bounds;
  }

  @Override
  public Map<String, Integer> getSpeciesIndexMap() {
    return new HashMap<>(spiIndexMap);
  }

  @Override
  public Map<String, Integer> getReactionsIndexMap() {
    return new HashMap<>(rxnIndexMap);
  }

  @Override
  public ConstraintBasedModel build() {
    logger.info("{} x {}", spiIndexMap.size(), rxnIndexMap.size());
    List<Range> bounds = new ArrayList<> ();
    double[][] bs = getBounds();
    for (int i = 0; i < rxnIndexMap.size(); i++) {
      bounds.add(new Range(bs[i][0], bs[i][1]));
    }
    return new ConstraintBasedModel(getMatrix(), bounds, spiIndexMap, rxnIndexMap);
  }

}
