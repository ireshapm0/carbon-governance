/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.governance.api.common;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifactImpl;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.pagination.Paginate;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Base Manager Functionality which can be used by any Artifact Manager instance.
 */
@SuppressWarnings("unused")
public class GovernanceArtifactManager {

    private static final Log log = LogFactory.getLog(GovernanceArtifactManager.class);
    private Registry registry;
    private String mediaType;
    private String artifactNameAttribute;
    private String artifactNamespaceAttribute;
    private String artifactElementRoot;
    private String artifactElementNamespace;
    private String pathExpression;
    private String lifecycle;
    private List<Association> relationshipDefinitions;
    private List<Map> validationAttributes;

    /**
     * Constructor accepting an instance of the registry, and also details on the type of manager.
     *
     * @param registry                   the instance of the registry.
     * @param mediaType                  the media type of resources being saved or fetched.
     * @param artifactNameAttribute      the attribute that specifies the name of the artifact.
     * @param artifactNamespaceAttribute the attribute that specifies the namespace of the artifact.
     * @param artifactElementRoot        the attribute that specifies the root artifact element.
     * @param artifactElementNamespace   the attribute that specifies the artifact element's
     *                                   namespace.
     * @param pathExpression             the expression that can be used to compute where to store
     *                                   the artifact.
     * @param relationshipDefinitions    the relationship definitions for the types of associations
     *                                   that will be created when the artifact gets updated.
     */
    public GovernanceArtifactManager(Registry registry, String mediaType,
                                     String artifactNameAttribute,
                                     String artifactNamespaceAttribute, String artifactElementRoot,
                                     String artifactElementNamespace, String pathExpression,
                                     Association[] relationshipDefinitions) {
        this.registry = registry;
        this.mediaType = mediaType;
        this.artifactNameAttribute = artifactNameAttribute;
        this.artifactNamespaceAttribute = artifactNamespaceAttribute;
        this.artifactElementRoot = artifactElementRoot;
        this.artifactElementNamespace = artifactElementNamespace;
        this.pathExpression = pathExpression;
        this.relationshipDefinitions = Arrays.asList(relationshipDefinitions);
    }

    /**
     * Constructor accepting an instance of the registry, and also details on the type of manager.
     *
     * @param registry                   the instance of the registry.
     * @param mediaType                  the media type of resources being saved or fetched.
     * @param artifactNameAttribute      the attribute that specifies the name of the artifact.
     * @param artifactNamespaceAttribute the attribute that specifies the namespace of the artifact.
     * @param artifactElementRoot        the attribute that specifies the root artifact element.
     * @param artifactElementNamespace   the attribute that specifies the artifact element's
     *                                   namespace.
     * @param pathExpression             the expression that can be used to compute where to store
     *                                   the artifact.
     * @param lifecycle                  the lifecycle name which associated with the artifacts
     * @param validationAttributes       the validations for artifact attributes
     * @param relationshipDefinitions    the relationship definitions for the types of associations
     *                                   that will be created when the artifact gets updated.
     *
     */
    public GovernanceArtifactManager(Registry registry, String mediaType,
                                     String artifactNameAttribute,
                                     String artifactNamespaceAttribute, String artifactElementRoot,
                                     String artifactElementNamespace, String pathExpression,
                                     String lifecycle, List<Map> validationAttributes,
                                     Association[] relationshipDefinitions) {
        this.registry = registry;
        this.mediaType = mediaType;
        this.artifactNameAttribute = artifactNameAttribute;
        this.artifactNamespaceAttribute = artifactNamespaceAttribute;
        this.artifactElementRoot = artifactElementRoot;
        this.artifactElementNamespace = artifactElementNamespace;
        this.pathExpression = pathExpression;
        this.lifecycle = lifecycle;
        this.validationAttributes = validationAttributes;
        this.relationshipDefinitions = Arrays.asList(relationshipDefinitions);
    }

    /**
     * Creates a new artifact from the given qualified name.
     *
     * @return the artifact added.
     * @throws GovernanceException if the operation failed.
     */
    public GovernanceArtifact newGovernanceArtifact() throws GovernanceException {
        return GovernanceArtifactImpl.create(registry, UUID.randomUUID().toString());
    }

    /**
     * Creates a new artifact from the given content.
     *
     * @param content the artifact content.
     *
     * @return the artifact added.
     * @throws GovernanceException if the operation failed.
     */
    public GovernanceArtifact newGovernanceArtifact(OMElement content) throws GovernanceException {
        return GovernanceArtifactImpl.create(registry, UUID.randomUUID().toString(), content);
    }

    /**
     * Adds the given artifact to the registry. Please do not use this method to update an existing
     * artifact use the update method instead. If this method is used to update an existing
     * artifact, all existing properties (such as lifecycle details) will be removed from the
     * existing artifact.
     *
     * @param artifact the artifact.
     *
     * @throws GovernanceException if the operation failed.
     */
    public void addGovernanceArtifact(GovernanceArtifact artifact) throws GovernanceException {
        // adding the attributes for name, namespace + artifact
        if (artifact.getQName() == null || artifact.getQName().getLocalPart() == null) {
            String msg = "A valid qualified name was not set for this artifact";
            log.error(msg);
            throw new GovernanceException(msg);
        }
        validateArtifact(artifact);
        String artifactName = artifact.getQName().getLocalPart();
        artifact.setAttributes(artifactNameAttribute,
                new String[]{artifactName});
        // namespace can be null
        String namespace = artifact.getQName().getNamespaceURI();
        if (artifactNamespaceAttribute != null) {
            artifact.setAttributes(artifactNamespaceAttribute,
                    new String[]{namespace});
        }

        ((GovernanceArtifactImpl)artifact).associateRegistry(registry);
        boolean succeeded = false;
        try {
            registry.beginTransaction();
            Resource resource = registry.newResource();

            resource.setMediaType(mediaType);
            setContent(artifact, resource);
            // the artifact will not actually stored in the tmp path.
            String path = GovernanceUtils.getPathFromPathExpression(
                    pathExpression, artifact);

            if(registry.resourceExists(path)){
                throw new GovernanceException("Governance artifact " + artifactName + " already exists at " + path);
            }

            String artifactId = artifact.getId();
            resource.setUUID(artifactId);
            registry.put(path, resource);

            if (lifecycle != null){
                registry.associateAspect(path, lifecycle);
            }

            ((GovernanceArtifactImpl)artifact).updatePath();
//            artifact.setId(resource.getUUID()); //This is done to get the UUID of a existing resource.
            addRelationships(path, artifact);

            succeeded = true;
        }
        catch (RegistryException e) {
            String msg;
            if (artifact.getPath() != null) {
                msg = "Failed to add artifact: artifact id: " + artifact.getId() +
                        ", path: " + artifact.getPath() + ". " + e.getMessage();
            } else {
                msg = "Failed to add artifact: artifact id: " + artifact.getId() +
                        ". " + e.getMessage();
            }
            log.error(msg, e);
            throw new GovernanceException(msg, e);
        } finally {
            if (succeeded) {
                try {
                    registry.commitTransaction();
                } catch (RegistryException e) {
                    String msg ;
                    if (artifact.getPath() != null) {
                        msg = "Error in committing transactions. Failed to add artifact: artifact " +
                                "id: " + artifact.getId() + ", path: " + artifact.getPath() + ".";
                    } else {
                        msg = "Error in committing transactions. Failed to add artifact: artifact " +
                                "id: " + artifact.getId() + ".";
                    }
                    log.error(msg, e);
                }
            } else {
                try {
                    registry.rollbackTransaction();
                } catch (RegistryException e) {
                    String msg =
                            "Error in rolling back transactions. Failed to add artifact: " +
                                    "artifact id: " + artifact.getId() + ", path: " +
                                    artifact.getPath() + ".";
                    log.error(msg, e);
                }
            }
        }
    }

    private void addRelationships(String path, GovernanceArtifact artifact)
            throws RegistryException {
        Map<String, AssociationInteger> typeMap =
                new LinkedHashMap<String, AssociationInteger>();
        for (Association relationship : relationshipDefinitions) {
            String type = relationship.getAssociationType();
            String source = relationship.getSourcePath();
            String target = relationship.getDestinationPath();
            if (typeMap.containsKey(type)) {
                AssociationInteger associationInteger = typeMap.get(type);
                if (source == null) {
                    if (associationInteger.getInteger() < 0) {
                        associationInteger.setInteger(0);
                    }
                    for (String targetPath :
                            GovernanceUtils.getPathsFromPathExpression(target, artifact)) {
                        associationInteger.getAssociations().add(
                                new Association(path, targetPath, type));
                    }
                } else if (target == null) {
                    if (associationInteger.getInteger() > 0) {
                        associationInteger.setInteger(0);
                    }
                    for (String sourcePath :
                            GovernanceUtils.getPathsFromPathExpression(source, artifact)) {
                        associationInteger.getAssociations().add(
                                new Association(sourcePath, path, type));
                    }
                }
            } else {
                AssociationInteger associationInteger = new AssociationInteger();
                if (source == null) {
                    associationInteger.setInteger(1);
                    for (String targetPath :
                            GovernanceUtils.getPathsFromPathExpression(target, artifact)) {
                        associationInteger.getAssociations().add(
                                new Association(path, targetPath, type));
                    }
                } else if (target == null) {
                    associationInteger.setInteger(-1);
                    for (String sourcePath :
                            GovernanceUtils.getPathsFromPathExpression(source, artifact)) {
                        associationInteger.getAssociations().add(
                                new Association(sourcePath, path, type));
                    }
                }
                typeMap.put(type, associationInteger);
            }
        }
        for (Map.Entry<String, AssociationInteger> e : typeMap.entrySet()) {
            AssociationInteger value = e.getValue();
            List<Association> associations = value.getAssociations();
            fixAssociations(path, e.getKey(), value.getInteger() >= 0, value.getInteger() <= 0,
                    associations.toArray(new Association[associations.size()]));
        }
    }

    private void fixAssociations(String path, String type, boolean isSource, boolean isTarget,
                                 Association[] toAdd)
            throws RegistryException {
        final String SEPARATOR = ":";
        // Get the existing association list which is related to the current operation
        Set<String> existingSet = new HashSet<String>();
        for (Association association : registry.getAllAssociations(path)) {
            if (type.equals(association.getAssociationType()) &&
                    ((isSource && association.getSourcePath().equals(path)) ||
                            (isTarget && association.getDestinationPath().equals(path)))) {
                existingSet.add(association.getSourcePath() + SEPARATOR +
                        association.getDestinationPath() +
                        SEPARATOR + association.getAssociationType());
            }
        }

        // Get the updated association list from the projectGroup object
        Set<String> updatedSet = new HashSet<String>();
        for (Association association : toAdd) {
            updatedSet.add(association.getSourcePath() + SEPARATOR +
                    association.getDestinationPath() +
                    SEPARATOR + association.getAssociationType());
        }

        Set<String> removedAssociations = new HashSet<String>(existingSet);
        removedAssociations.removeAll(updatedSet);

        Set<String> newAssociations = new HashSet<String>(updatedSet);
        newAssociations.removeAll(existingSet);

        for (String removedAssociation : removedAssociations) {
            String[] params = removedAssociation.split(SEPARATOR);
            registry.removeAssociation(params[0], params[1], params[2]);
        }

        for (String newAssociation : newAssociations) {
            String[] params = newAssociation.split(SEPARATOR);
            registry.addAssociation(params[0], params[1], params[2]);
        }
    }

    /**
     * Updates the given artifact on the registry.
     *
     * @param artifact the artifact.
     *
     * @throws GovernanceException if the operation failed.
     */
    public void updateGovernanceArtifact(GovernanceArtifact artifact) throws GovernanceException {
        boolean succeeded = false;
        try {
            registry.beginTransaction();
            validateArtifact(artifact);
            GovernanceArtifact oldArtifact = getGovernanceArtifact(artifact.getId());
            // first check for the old artifact and remove it.
            String oldPath = null;
            if (oldArtifact != null) {
                QName oldName = oldArtifact.getQName();
                if (!oldName.equals(artifact.getQName())) {
                    String temp = oldArtifact.getPath();
                    // then it is analogue to moving the resource for the new location
                    // so just delete the old path
                    registry.delete(temp);

                    String artifactName = artifact.getQName().getLocalPart();
                    artifact.setAttributes(artifactNameAttribute,
                            new String[]{artifactName});
                    String namespace = artifact.getQName().getNamespaceURI();
                    if (artifactNamespaceAttribute != null) {
                        artifact.setAttributes(artifactNamespaceAttribute,
                                new String[]{namespace});
                    }
                } else {
                    oldPath = oldArtifact.getPath();
                }
            } else {
                throw new GovernanceException("No artifact found for the artifact id :" + artifact.getId() + ".");
            }

            String artifactId =  artifact.getId();
            Resource resource = registry.newResource();
            resource.setMediaType(mediaType);
            setContent(artifact, resource);
            String path = GovernanceUtils.getPathFromPathExpression(
                    pathExpression, artifact);

            if (oldPath != null) {
                path = oldPath;
            }
            if (registry.resourceExists(path)) {
                Resource oldResource = registry.get(path);
                Properties properties = (Properties) oldResource.getProperties().clone();

                // first clone existing properties
                resource.setProperties(properties);

                // then set updated properties
                String[] attributeKeys = artifact.getAttributeKeys();
                if (attributeKeys != null) {
                    for (String aggregatedKey : attributeKeys) {
                        if (!aggregatedKey.equals(artifactNameAttribute) &&
                                !aggregatedKey.equals(artifactNamespaceAttribute)) {
                            resource.setProperty(aggregatedKey,
                                    Arrays.asList(artifact.getAttributes(aggregatedKey)));
                        }
                    }
                }

                //persisting resource description at artifact update
                String description = oldResource.getDescription();
                if(description != null) {
                    resource.setDescription(description);
                }

                String oldContent;
                Object content = oldResource.getContent();
                if (content instanceof String) {
                    oldContent = (String) content;
                } else {
                    oldContent = new String((byte[]) content);
                }
                String newContent;
                content = resource.getContent();
                if (content instanceof String) {
                    newContent = (String) content;
                } else {
                    newContent = new String((byte[]) content);
                }
                if (newContent.equals(oldContent)) {
                    artifact.setId(oldResource.getUUID());
                    addRelationships(path, artifact);
                    succeeded = true;
                    return;
                }
            }
            resource.setUUID(artifactId);
            registry.put(path, resource);
//            artifact.setId(resource.getUUID()); //This is done to get the UUID of a existing resource.
            addRelationships(path, artifact);
            ((GovernanceArtifactImpl)artifact).updatePath(artifactId);
            succeeded = true;
        } catch (RegistryException e) {
            if (e instanceof GovernanceException) {
                throw (GovernanceException) e;
            }
            String msg;
            if (artifact.getPath() != null) {
                msg = "Error in updating the artifact, artifact id: " + artifact.getId() +
                        ", artifact path: " + artifact.getPath() + "." + e.getMessage() + ".";
            } else {
                msg = "Error in updating the artifact, artifact id: " + artifact.getId() +
                        "." + e.getMessage() + ".";
            }
            log.error(msg, e);
            throw new GovernanceException(msg, e);
        } finally {
            if (succeeded) {
                try {
                    registry.commitTransaction();
                } catch (RegistryException e) {
                    String msg;
                    if (artifact.getPath() != null) {
                        msg = "Error in committing transactions. Update artifact failed: artifact " +
                                "id: " + artifact.getId() + ", path: " + artifact.getPath() + ".";
                    } else {
                        msg = "Error in committing transactions. Update artifact failed: artifact " +
                                "id: " + artifact.getId() + ".";
                    }

                    log.error(msg, e);
                }
            } else {
                try {
                    registry.rollbackTransaction();
                } catch (RegistryException e) {
                    String msg =
                            "Error in rolling back transactions. Update artifact failed: " +
                                    "artifact id: " + artifact.getId() + ", path: " +
                                    artifact.getPath() + ".";
                    log.error(msg, e);
                }
            }
        }
    }

    /**
     * Fetches the given artifact on the registry.
     *
     * @param artifactId the identifier of the artifact.
     *
     * @return the artifact.
     * @throws GovernanceException if the operation failed.
     */
    public GovernanceArtifact getGovernanceArtifact(String artifactId) throws GovernanceException {
        return GovernanceUtils.retrieveGovernanceArtifactById(registry, artifactId);
    }

    /**
     * Removes the given artifact from the registry.
     *
     * @param artifactId the identifier of the artifact.
     *
     * @throws GovernanceException if the operation failed.
     */
    public void removeGovernanceArtifact(String artifactId) throws GovernanceException {
        GovernanceUtils.removeArtifact(registry, artifactId);
    }

    /**
     * Sets content of the given artifact to the given resource on the registry.
     *
     * @param artifact the artifact.
     * @param resource the content resource.
     *
     * @throws GovernanceException if the operation failed.
     */
    protected void setContent(GovernanceArtifact artifact, Resource resource) throws
            GovernanceException {
        try {
            if (artifact instanceof GenericArtifact) {
                Object content = ((GenericArtifact) artifact).getContent();
                if (content != null) {
                    setContentAndProperties(artifact, resource, content);
                    return;
                }
            }
            
            OMNamespace namespace = OMAbstractFactory.getOMFactory().createOMNamespace(artifactElementNamespace, "");
            Map<String, HashMap> mainElementMap = new HashMap<String, HashMap>();
            final String defaultUUID = UUID.randomUUID().toString();
            String[] attributeKeys = artifact.getAttributeKeys();
            if (attributeKeys != null) {
                for (String aggregatedKey : attributeKeys) {
                    String[] keys = aggregatedKey.split("_");
                    String key = null;
                    for (int i = 0; i < keys.length; i++) {
                        key = keys[i];
                        if (mainElementMap.get(key) == null) {
                            mainElementMap.put(key, new HashMap<String, String[]>());
                        }

                        // Handing the situations where we don't have '_' in aggregatedKey
                        // and assume we hare having only one '_" in aggregatedKey and not more
                        if (keys.length > 1) {
                            break;
                        }
                    }
                    String[] attributeValues = artifact.getAttributes(aggregatedKey);
                    String elementName = keys[keys.length - 1];
                    if (keys.length > 1) {
                        mainElementMap.get(key).put(elementName, attributeValues);
                    } else {
                        mainElementMap.get(key).put(defaultUUID, attributeValues);
                    }
                }
            	
            }
            
            OMElement contentElement = OMAbstractFactory.getOMFactory().createOMElement(artifactElementRoot, namespace);
            
            Iterator it = mainElementMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, HashMap> pairs = (Map.Entry)it.next();
                
                Map<String, Map>  subElementMap = pairs.getValue();
                Iterator subit = subElementMap.entrySet().iterator();
                int size = 0;
                boolean isOptionText =  false;
                while (subit.hasNext()) {
                    Map.Entry<String, String[]> subpairs = (Map.Entry) subit.next();
                    if (size < subpairs.getValue().length) {
                        size = subpairs.getValue().length;
                    }
                    // TODO : This is temporary fix for option-text related issue, removing option-text next major release.
                    if (subpairs.getKey().endsWith("entry")) {
                        isOptionText = true;
                    }
                }
                
                // TODO : This is temporary fix for option-text related issue, removing option-text next major release.
                if (isOptionText) {
                    OMElement keyElement = OMAbstractFactory.getOMFactory().createOMElement(pairs.getKey(), namespace);
                    for (int i = 0; i < size; i++) {
                        keyElement = getSubElementContent(subElementMap, namespace, defaultUUID, keyElement, i);
                    }
                    contentElement.addChild(keyElement);
                } else {
                    for (int i = 0; i < size; i++) {
                        OMElement keyElement = OMAbstractFactory.getOMFactory().createOMElement(pairs.getKey(), namespace);
                        keyElement = getSubElementContent(subElementMap, namespace, defaultUUID, keyElement, i);
                        contentElement.addChild(keyElement);
                    }
                }
            }
            String updatedContent = GovernanceUtils.serializeOMElement(contentElement);
            setContentAndProperties(artifact, resource, updatedContent);
            
        } catch (RegistryException e) {
            String msg;
            if (artifact.getPath() != null) {
                msg = "Error in saving attributes for the artifact. id: " + artifact.getId() +
                        ", path: " + artifact.getPath() + ".";
            } else {
                msg = "Error in saving attributes for the artifact. id: " + artifact.getId() + ".";
            }
            log.error(msg, e);
            throw new GovernanceException(msg, e);
        }
    }

    private void setContentAndProperties(GovernanceArtifact artifact, Resource resource, Object content)
            throws RegistryException {
        resource.setContent(content);
        String[] attributeKeys = artifact.getAttributeKeys();
        if (attributeKeys != null) {
            for (String aggregatedKey : attributeKeys) {
                if (!aggregatedKey.equals(artifactNameAttribute) &&
                        !aggregatedKey.equals(artifactNamespaceAttribute)) {
                    resource.setProperty(aggregatedKey,
                            Arrays.asList(artifact.getAttributes(aggregatedKey)));
                }
            }
        }
    }


    /**
     * Finds all artifacts matching the given filter criteria.
     *
     * @param criteria the filter criteria to be matched.
     *
     * @return the artifacts that match.
     * @throws GovernanceException if the operation failed.
     */
    public GovernanceArtifact[] findGovernanceArtifacts(Map<String, List<String>> criteria)
            throws GovernanceException {
        List<GovernanceArtifact> artifacts;
        artifacts = GovernanceUtils.findGovernanceArtifacts(criteria != null ? criteria :
                Collections.<String, List<String>>emptyMap(), registry, mediaType);
        if (artifacts != null) {
            return artifacts.toArray(new GovernanceArtifact[artifacts.size()]);
        } else {
            return new GovernanceArtifact[0];
        }
    }

    /**
     * Finds all artifacts matching the given filter criteria.
     *
     * @param criteria the filter criteria to be matched.
     *
     * @return the artifacts that match.
     * @throws GovernanceException if the operation failed.
     */
    public GovernanceArtifact[] findGovernanceArtifacts(GovernanceArtifactFilter criteria)
            throws GovernanceException {
        List<GovernanceArtifact> artifacts = new ArrayList<GovernanceArtifact>();
        for (GovernanceArtifact artifact : getAllGovernanceArtifacts()) {
            if (artifact != null) {
                if (criteria.matches(artifact)) {
                    artifacts.add(artifact);
                }
            }
        }
        return artifacts.toArray(new GovernanceArtifact[artifacts.size()]);
    }

    /**
     * Finds all artifacts of a given type on the registry.
     *
     * @return all artifacts of the given type on the registry.
     * @throws GovernanceException if the operation failed.
     */
    public GovernanceArtifact[] getAllGovernanceArtifacts() throws GovernanceException {
        List<String> paths = getPaginatedGovernanceArtifacts();

        GovernanceArtifact[] artifacts = new GovernanceArtifact[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            artifacts[i] = GovernanceUtils.retrieveGovernanceArtifactByPath(registry, paths.get(i));
        }
        return artifacts;
    }

    @Paginate("getPaginatedGovernanceArtifacts")
    public List<String> getPaginatedGovernanceArtifacts() throws GovernanceException {
        List<String> paths =
                Arrays.asList(GovernanceUtils.getResultPaths(registry,
                        mediaType));
        Collections.sort(paths, new Comparator<String>() {
            public int compare(String o1, String o2) {
                Long l1 = -1l;
                Long l2 = -1l;

                String temp1 = RegistryUtils.getParentPath(o1);
                String temp2 = RegistryUtils.getParentPath(o2);
                try {
                    l1 = Long.parseLong(
                            RegistryUtils.getResourceName(temp1));
                    l2 = Long.parseLong(
                            RegistryUtils.getResourceName(temp2));
                } catch (NumberFormatException ignore) {

                }

                // First order by name
                int result = RegistryUtils.getResourceName(temp1).compareToIgnoreCase(
                        RegistryUtils.getResourceName(temp2));
                if (result != 0) {
                    return result;
                }
                // Then order by namespace
                result = temp1.compareToIgnoreCase(temp2);
                if (result != 0) {
                    return result;
                }
                // Finally by version
                return l2.compareTo(l1);
            }
        });
        return paths;
    }

    /**
     * Finds all identifiers of the artifacts on the registry.
     *
     * @return an array of identifiers of the artifacts.
     * @throws GovernanceException if the operation failed.
     */
    public String[] getAllGovernanceArtifactIds() throws GovernanceException {
        GovernanceArtifact[] artifacts = getAllGovernanceArtifacts();
        String[] artifactIds = new String[artifacts.length];
        for (int i = 0; i < artifacts.length; i++) {
            artifactIds[i] = artifacts[i].getId();
        }
        return artifactIds;
    }

    private static class AssociationInteger {

        private List<Association> associations = new LinkedList<Association>();
        private Integer integer;

        public List<Association> getAssociations() {
            return associations;
        }

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }
    }

    /**
     * Retrieve all the governance artifacts which associated with the given lifecycle
     *
     * @param lcName Name of the lifecycle
     *
     * @return GovernanceArtifact array
     * @throws GovernanceException
     */
    public GovernanceArtifact[] getAllGovernanceArtifactsByLifecycle(String lcName) throws GovernanceException {
        String[] paths = GovernanceUtils.getAllArtifactPathsByLifecycle(registry, lcName, mediaType);
        if (paths == null){
            return new GovernanceArtifact[0];
        }
        GovernanceArtifact[] artifacts = new GovernanceArtifact[paths.length];
        for(int i=0; i<paths.length; i++){
            artifacts[i] = GovernanceUtils.retrieveGovernanceArtifactByPath(registry, paths[i]);
        }

        return artifacts;
    }

    /**
     * Retrieve all the governance artifacts which associated with the given lifecycle in the given lifecycle state
     *
     * @param lcName  Name of the lifecycle
     * @param lcState Name of the current lifecycle state
     *
     * @return GovernanceArtifact array
     * @throws GovernanceException
     */
    public GovernanceArtifact[] getAllGovernanceArtifactsByLIfecycleStatus(String lcName, String lcState)
            throws GovernanceException {
        String[] paths = GovernanceUtils.getAllArtifactPathsByLifecycleState(registry, lcName, lcState, mediaType);
        if (paths == null){
            return new GovernanceArtifact[0];
        }
        GovernanceArtifact[] artifacts = new GovernanceArtifact[paths.length];
        for(int i=0; i<paths.length; i++){
            artifacts[i] = GovernanceUtils.retrieveGovernanceArtifactByPath(registry, paths[i]);
        }

        return artifacts;
    }

    private void validateArtifact(GovernanceArtifact artifact)
            throws GovernanceException{
        if(validationAttributes == null){
            return;
        }
        Map<String, Object> map;
        for (int i=0; i<validationAttributes.size(); ++i) {
            map = validationAttributes.get(i);
            String value = "";
            String prop = (String)map.get("properties");
            List<String> keys = (List<String>)map.get("keys");

            if (prop != null && "unbounded".equals(prop)) {
                //assume there are only 1 key
                String[] values = artifact.getAttributes((String)keys.get(0));
                if (values != null) {
                    for (int j=0; j<values.length; ++j) {
                        if (!values[j].matches((String)map.get("regexp"))) {
                            //return an exception to stop adding artifact
                            throw new GovernanceException((String)map.get("name") + " doesn't match regex: " +
                                    (String)map.get("Regexp"));
                        }
                    }
                }
            } else {
                for (int j=0; j<keys.size(); ++j) {
                    String v = artifact.getAttribute(keys.get(j));
                    if (j != 0) value += ":";
                    value += (v == null ? "" : v);
                }
                if (!value.matches((String)map.get("regexp"))) {
                    //return an exception to stop adding artifact
                    throw new GovernanceException((String)map.get("name") + " doesn't match regex: " +
                            (String)map.get("Regexp"));
                }
            }
        }
    }
    
    private OMElement getSubElementContent (Map<String, Map> subElementMap, OMNamespace namespace, String defaultUUID, OMElement keyElement, int posotion) {
        Iterator iterator = subElementMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String[]> subpairs = (Map.Entry)iterator.next();
            String value ;
            try {
                value = subpairs.getValue()[posotion];
            } catch (Exception ex) {
                value = null;
            }

            //We have defaultUUID for non '_' scenario in this case remove defaultUUID and append it to the
            //parent element.
            if (subpairs.getKey().equals(defaultUUID)) {
                OMText textElement = OMAbstractFactory.getOMFactory().createOMText(value);
                keyElement.addChild(textElement);
            } else {
                OMElement subkeyElement = OMAbstractFactory.getOMFactory().createOMElement(subpairs.getKey(), namespace);
                OMText textElement = OMAbstractFactory.getOMFactory().createOMText(value);
                subkeyElement.addChild(textElement);
                keyElement.addChild(subkeyElement);
            } 
        }
        return keyElement;
    }
}
