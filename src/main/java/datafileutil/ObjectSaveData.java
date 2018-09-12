
package datafileutil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import us.kbase.common.service.UObject;


/**
 * <p>Original spec-file type: ObjectSaveData</p>
 * <pre>
 * An object and associated data required for saving.
 *     Required parameters:
 *     type - the workspace type string for the object. Omit the version
 *         information to use the latest version.
 *     data - the object data.
 *     
 *     Optional parameters:
 *     One of an object name or id. If no name or id is provided the name
 *         will be set to 'auto' with the object id appended as a string,
 *         possibly with -\d+ appended if that object id already exists as a
 *         name.
 *     name - the name of the object.
 *     objid - the id of the object to save over.
 *     meta - arbitrary user-supplied metadata for the object,
 *         not to exceed 16kb; if the object type specifies automatic
 *         metadata extraction with the 'meta ws' annotation, and your
 *         metadata name conflicts, then your metadata will be silently
 *         overwritten.
 *     hidden - true if this object should not be listed when listing
 *         workspace objects.
 *     extra_provenance_input_refs - (optional) if set, these refs will
 *         be appended to the primary ProveanceAction input_ws_objects
 *         reference list. In general, if the input WS object ref was
 *         passed in from a narrative App, this will be set for you.
 *         However, there are cases where the object ref passed to
 *         the App is a container, and you are operating on a member
 *         or subobject of the container, in which case to maintain
 *         that direct mapping to those subobjects in the provenance
 *         of new objects, you can provide additional object refs
 *         here. For example, if the input is a ReadsSet, and your
 *         App creates a new WS object for each read library in the
 *         set, you may want a direct reference from each new WS
 *         object not only to the set, but also to the individual
 *         read library.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "type",
    "data",
    "name",
    "objid",
    "meta",
    "hidden",
    "extra_provenance_input_refs"
})
public class ObjectSaveData {

    @JsonProperty("type")
    private java.lang.String type;
    @JsonProperty("data")
    private UObject data;
    @JsonProperty("name")
    private java.lang.String name;
    @JsonProperty("objid")
    private Long objid;
    @JsonProperty("meta")
    private Map<String, String> meta;
    @JsonProperty("hidden")
    private Long hidden;
    @JsonProperty("extra_provenance_input_refs")
    private List<String> extraProvenanceInputRefs;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("type")
    public java.lang.String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(java.lang.String type) {
        this.type = type;
    }

    public ObjectSaveData withType(java.lang.String type) {
        this.type = type;
        return this;
    }

    @JsonProperty("data")
    public UObject getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(UObject data) {
        this.data = data;
    }

    public ObjectSaveData withData(UObject data) {
        this.data = data;
        return this;
    }

    @JsonProperty("name")
    public java.lang.String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(java.lang.String name) {
        this.name = name;
    }

    public ObjectSaveData withName(java.lang.String name) {
        this.name = name;
        return this;
    }

    @JsonProperty("objid")
    public Long getObjid() {
        return objid;
    }

    @JsonProperty("objid")
    public void setObjid(Long objid) {
        this.objid = objid;
    }

    public ObjectSaveData withObjid(Long objid) {
        this.objid = objid;
        return this;
    }

    @JsonProperty("meta")
    public Map<String, String> getMeta() {
        return meta;
    }

    @JsonProperty("meta")
    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public ObjectSaveData withMeta(Map<String, String> meta) {
        this.meta = meta;
        return this;
    }

    @JsonProperty("hidden")
    public Long getHidden() {
        return hidden;
    }

    @JsonProperty("hidden")
    public void setHidden(Long hidden) {
        this.hidden = hidden;
    }

    public ObjectSaveData withHidden(Long hidden) {
        this.hidden = hidden;
        return this;
    }

    @JsonProperty("extra_provenance_input_refs")
    public List<String> getExtraProvenanceInputRefs() {
        return extraProvenanceInputRefs;
    }

    @JsonProperty("extra_provenance_input_refs")
    public void setExtraProvenanceInputRefs(List<String> extraProvenanceInputRefs) {
        this.extraProvenanceInputRefs = extraProvenanceInputRefs;
    }

    public ObjectSaveData withExtraProvenanceInputRefs(List<String> extraProvenanceInputRefs) {
        this.extraProvenanceInputRefs = extraProvenanceInputRefs;
        return this;
    }

    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public java.lang.String toString() {
        return ((((((((((((((((("ObjectSaveData"+" [type=")+ type)+", data=")+ data)+", name=")+ name)+", objid=")+ objid)+", meta=")+ meta)+", hidden=")+ hidden)+", extraProvenanceInputRefs=")+ extraProvenanceInputRefs)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
