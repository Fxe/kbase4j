package me.fxe.kbase;

public class KBaseConfig {
  public static final String TEMPLATE_WORKSPACE = "NewKBaseModelTemplates";
  
  public static final String T_G_POS = "GramPosModelTemplate";
  public static final String T_G_NEG = "GramNegModelTemplate";
  public static final String T_CORE = "CoreModelTemplate";
  
  public static final String REF_EMPTY_GENOME = "PlantSEED/Empty";
  
  public static final String REF_TEMPLATE_G_NEG = TEMPLATE_WORKSPACE + "/" + T_G_NEG;
  public static final String REF_TEMPLATE_G_POS = TEMPLATE_WORKSPACE + "/" + T_G_POS;
  public static final String REF_TEMPLATE_CORE =  TEMPLATE_WORKSPACE + "/" + T_CORE;
}
