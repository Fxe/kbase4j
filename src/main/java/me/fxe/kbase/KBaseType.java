package me.fxe.kbase;

public enum KBaseType {
  FBAModel("KBaseFBA.FBAModel"),
  NewModelTemplate("KBaseFBA.NewModelTemplate"),
  Genome("KBaseGenomes.Genome"), 
  KBaseBiochemMedia("KBaseBiochem.Media"),
  
  Any("Any");
  
  private final String value;
  
  KBaseType(String v) {
    value = v;
  }
  
  public String value() { return value; } 
}
