// Copyright (c) 1998-2015 Core Solutions Limited. All rights reserved.
// ============================================================================
// CURRENT VERSION CNT.5.8.0
// ============================================================================
// CHANGE LOG
// CNT.5.8.0 : 2015-02-05, arthur.ou, CNT-16347
// CNT.5.8.0 : 2015-01-04, arthur.ou, CNT-14096
// CNT.5.0.074 : 2014-12-05, ivan.lo, CNT-14000
// CNT.5.0.063 : 2013-12-27, johnson.chen, CNT-11926
// CNT.5.0.063 : 2013-12-04, ace.li, CNT-11792
// CNT.5.0.040 : 2013-05-27, mark.lin, CNT-9388
// CNT.5.0.1 : 2012-08-17, mark.lin, CNT-5414
// ============================================================================
package com.core.cbx.hcl.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.core.cbx.action.constants.GeneralExceptionConstants;
import com.core.cbx.action.constants.GeneralFormConstants;
import com.core.cbx.action.exception.ActionException;
import com.core.cbx.action.workerAction.RebuildHclChildNodesWorkerAction;
import com.core.cbx.common.logging.CNTLogger;
import com.core.cbx.common.logging.LogFactory;
import com.core.cbx.data.DynamicEntityModel;
import com.core.cbx.data.constants.DataListType;
import com.core.cbx.data.constants.Hcl;
import com.core.cbx.data.constants.HclType;
import com.core.cbx.data.def.EntityDefManager;
import com.core.cbx.data.entity.DynamicEntity;
import com.core.cbx.data.exception.DataException;
import com.core.cbx.hcl.HclDef;
import com.core.cbx.hcl.HclNode;
import com.core.cbx.util.HclUtil;

/**
 *
 * @author mark.lin
 *
 */
public final class HclDocHelper {

    /*
     * - The temporary field 'navigation' is stored the current Child Nodes's 'fullLineage' move its 'code'.
     * - The temporary field 'level' is stored the current Child Nodes's 'Level'.
     */
    public static final String HCL_NAVIGATION = "navigation";
    public static final String HCL_LEVEL = "typeLevel";

    public static final String TOTAL_TYPE_LEVEL = "totalLevel";
    public static final String DATA_LIST_TYPE_MAP = "dataListTypeMap";

    public static final String SEPARATOR = "/";
    public static final String PARENT_FULL_LINEAGE = "parentFullLineage";
    private static final CNTLogger LOGGER = LogFactory.getLogger(HclDocHelper.class);

    private HclDocHelper() {
    }

    /**
     * Stored the total level and the data list type in different level of the [Hierarchical Code List Type] document.
     * @param doc
     * @throws ActionException
     * @throws DataException
     */
    public static void loadDataListTypeForHclDoc(final DynamicEntity doc) throws ActionException {
        final String hclTypeId = doc.getString(Hcl.HCL_TYPE_ID);

        if (StringUtils.isEmpty(hclTypeId)) {
            return;
        }

        DynamicEntity hclTypeDoc;
        try {
            hclTypeDoc = DynamicEntityModel.getFullEntity(HclType.ENTITY_NAME_HCL_TYPE, hclTypeId);
        } catch (final DataException e) {
            throw new ActionException(GeneralExceptionConstants.ACTION_EXCEPTION_000001,
                    "Fail to laod the document from CNT_HCL_TYPE where id = " + hclTypeId, e);
        }

        if (hclTypeDoc == null) {
            LOGGER.debug("Warning: The HclTypeDoc is empty.");
            return;
        }

        //+++++++++ <Key, Value> = <level, dataListType entity> +++++++++
        final Map<String, DynamicEntity>  dataListTypeMap = new HashMap<String, DynamicEntity>();
        final Collection<DynamicEntity> levels = hclTypeDoc.getEntityCollection(HclType.LEVELS);
        if (CollectionUtils.isNotEmpty(levels)) {
            final int totalLevel = levels.size();
            for (final DynamicEntity level : levels) {
                final String dataListTypeDD = level.getString(HclType.DATA_LIST_TYPE);
                if (StringUtils.isNotEmpty(dataListTypeDD)) {
                    try {
                        final DynamicEntity dataListTypeDoc = DynamicEntityModel.getFullEntity(
                                DataListType.ENTITY_NAME_DATA_LIST_TYPE,
                                dataListTypeDD);
                        dataListTypeMap.put(level.getString(HclType.TYPE_LEVEL), dataListTypeDoc);
                    }  catch (final DataException e) {
                        throw new ActionException(GeneralExceptionConstants.ACTION_EXCEPTION_000001,
                                "Fail to laod the data list type where id = " + dataListTypeDD, e);
                    }
                }
            }
            doc.putValue(TOTAL_TYPE_LEVEL, totalLevel);
            doc.putValue(DATA_LIST_TYPE_MAP, dataListTypeMap);
        }
    }

    /**
     * Initialized the temporary fields : 'Navigation'(lineage), 'Level'(typeLeve)
     * @param doc
     */
    public static void initHeaderFieldDefaultValue(final DynamicEntity doc) {
        doc.putValue(HCL_NAVIGATION, HclDocHelper.SEPARATOR);
        doc.putValue(HCL_LEVEL, 1L);
    }

    /**
     * Invoke in Service Layer.
     * @param doc
     * @throws ActionException
     */
    public static void handleForHclDoc(final DynamicEntity doc) throws ActionException {
        loadDataListTypeForHclDoc(doc);
        initHeaderFieldDefaultValue(doc);
    }

    /**
     * Invoke in Service Layer.
     * @param doc
     */
    public static void backupHclNodes(final DynamicEntity doc) {
        final Collection<DynamicEntity> hclNodes = doc.getEntityCollection(Hcl.HCL_NODES);
        doc.put(RebuildHclChildNodesWorkerAction.ORIGINAL_HCL_NODES_KEY, hclNodes);
    }

    /**
     * Retrieve the Child Nodes group by its parent Id
     * @param doc The Hierarchical Code List document
     * @param hclNodeParentId
     * @return
     * @deprecated Since 5.8.0, CNT-14096, replace by getHclNodesInGroupByParentCode(DynamicEntity, String)
     */
    @Deprecated
    public static List<DynamicEntity> getHclNodesInGroupByParentId(final DynamicEntity doc,
            final String hclNodeParentId) {
        final List<DynamicEntity> childNodesInGroup = new ArrayList<DynamicEntity>();
        final Collection<DynamicEntity> hclNodes = doc.getEntityCollection(Hcl.HCL_NODES);
        if (CollectionUtils.isNotEmpty(hclNodes)) {
            for (final DynamicEntity hclNode : hclNodes) {
                final String parentId = hclNode.getString(Hcl.PARENT_ID);
                if (StringUtils.equalsIgnoreCase(hclNodeParentId, parentId)) {
                    childNodesInGroup.add(hclNode);
                }
            }
        }
        return childNodesInGroup;
    }

    /**
     * Retrieve the Child Nodes group by its parent full code
     * if node's fullLineage is "/GM/GARDEN/ORNAMENTAL", it's parent full code is "/GM/GARDEN"
     * @param doc The Hierarchical Code List document
     * @param  parentFullLineage
     * @return
     */
    public static List<DynamicEntity> getHclNodesInGroupByParentCode(final DynamicEntity doc,
            final String parentFullLineage) {
        final List<DynamicEntity> childNodesInGroup = new ArrayList<DynamicEntity>();
        final Collection<DynamicEntity> hclNodes = doc.getEntityCollection(Hcl.HCL_NODES);
        if (CollectionUtils.isNotEmpty(hclNodes)) {
            for (final DynamicEntity hclNode : hclNodes) {
                final String fullLineage = hclNode.getString(Hcl.FULL_LINEAGE);
                // if parentFullLineage is "/", mean need to show record's which nodeLevel is 1
                if (StringUtils.length(parentFullLineage) == 1 && hclNode.getLong(Hcl.NODE_LEVEL) == 1) {
                    childNodesInGroup.add(hclNode);
                } else {
                    /**
                     *In level two, if the hclNode is completed, its fullLineage is (/parentFullLineage/its own code)
                     *In level two, if the hclNode is copy out and new node, its fullLineage is (/parentFullLineage),
                     *  will complete the fullLineage when user click triangle button
                     *  or "Navigation" or SavaAndConfirm button
                     */
                    final int nodeLevel = hclNode.getLong(Hcl.NODE_LEVEL).intValue();
                    final int countMatch = StringUtils.countMatches(fullLineage, HclDocHelper.SEPARATOR);
                    // node's fullLineage had been completed
                    if (nodeLevel == countMatch) {
                        final String fullParentCode
                                = StringUtils.substringBeforeLast(fullLineage, HclDocHelper.SEPARATOR);
                        if (StringUtils.equals(parentFullLineage, fullParentCode)) {
                            childNodesInGroup.add(hclNode);
                        }
                    } else {
                        // node's fullLieage hadn't been completed
                        if (StringUtils.equals(fullLineage, parentFullLineage)) {
                            childNodesInGroup.add(hclNode);
                        }
                    }
                }
            }
        }
        return childNodesInGroup;
    }

    /**
     * to reset headerHcl when create other module from item.
     * @param doc the DynamicEntity to be operated
     * @throws DataException
     */
    public static void resetHeaderHcl(final DynamicEntity doc) throws DataException {
        final String moduleCode = EntityDefManager.getModuleCodeByEntityName(doc.getEntityName());
        final DynamicEntity hclNodeEntity = doc.getEntity(GeneralFormConstants.HIERARCHY);
        final String hclId = hclNodeEntity.getString(Hcl.HCL_ID);
        final String fullLineage = hclNodeEntity.getString(Hcl.FULL_LINEAGE);
        final int ssl = HclUtil.getSslByModuleAndHclId(moduleCode, hclId);
        final String hclPath = HclUtil.getHclPathBySslAndFullLineage(fullLineage, ssl);
        if (hclPath != null) {
            final HclDef hclDef = HclUtil.findHclDefById(hclId);
            final HclNode[] hclNodes = hclDef.findNodesByFullLineage(hclPath);
            doc.putValue(GeneralFormConstants.HIERARCHY, hclNodes[hclNodes.length - 1].getHclNodeEntity());
        }
    }

    /**
     * @deprecated Since 5.8.0, CNT-13429, replace by getHclPathBySslAndFullLineage(String, int)
     */
    @Deprecated
    public static String getHclPathBySsl(final String lineage, final String hclNodeId, final int ssl)
            throws DataException {
         return HclUtil.getHclPathBySsl(lineage, hclNodeId, ssl);
    }

    public static String getHclPathBySslAndFullLineage(final String fullLineage, final int ssl)
            throws DataException {
         return HclUtil.getHclPathBySslAndFullLineage(fullLineage, ssl);
    }

    /**
     * update full line age of current hclNode and its child nodes
     */
    public static void updateNodesFullLineage(final DynamicEntity hclNode, final DynamicEntity doc) {
        final List<String> fullLineageList = new ArrayList<String>();
        final Collection<DynamicEntity> allHclNodes = doc.getEntityCollection(Hcl.HCL_NODES);
        for (final DynamicEntity node : allHclNodes) {
            fullLineageList.add(node.getString(Hcl.FULL_LINEAGE));
        }
        if (hclNode.isDeletedEntity()) {
            return;
        } else if (hclNode.isNewEntity()) {
            final Long nodeLevel = hclNode.getLong(Hcl.NODE_LEVEL);
            String fullLineage = hclNode.getString(Hcl.FULL_LINEAGE);
            final String tempCode = hclNode.getString(Hcl.CODE);
            final String verifyCode = StringUtils.substringAfterLast(fullLineage, HclDocHelper.SEPARATOR);
            if (!StringUtils.equals(verifyCode, tempCode)) {
                // if the hclNode is blank, no need to complete its fullLineage
                if (StringUtils.isBlank(tempCode)) {
                    return;
                } else if (fullLineage.length() > 1) {
                    //if the hclNode is already complete, but update its code
                    if (StringUtils.countMatches(fullLineage, HclDocHelper.SEPARATOR) == nodeLevel) {
                        final String oldFullLineage = new String(fullLineage);
                        fullLineage = StringUtils.substringBeforeLast(fullLineage, HclDocHelper.SEPARATOR)
                                + HclDocHelper.SEPARATOR + tempCode;
                        //handle logic
                        if (!StringUtils.equals(fullLineage, oldFullLineage)) {
                            handleHclChildNode(fullLineage, oldFullLineage, doc);
                        }
                    } else {
                        // if the hclNode never be completed, its fullLineage is parentFullLineage.
                        // should append "/" and its code to complete its fullLineage
                        final String tempFullLineage = fullLineage + HclDocHelper.SEPARATOR + tempCode;
                        // if in level three(or level two), switch by "Navigation" will complete every nodes'
                        // fullLineage in GetSameLevelHclNodesAction, if this layer have same code, can not
                        // build the same fullLineage in same layer hclNode, it will ruin the follow logic.
                        if (!fullLineageList.contains(tempFullLineage)) {
                            fullLineage = tempFullLineage;
                        }
                    }
                } else {
                    fullLineage = fullLineage + tempCode;
                }
                hclNode.put(Hcl.FULL_LINEAGE, fullLineage);
            }
        }
        if (hclNode.isModifiedEntity()) {
            if (hclNode.getModifiedValueMap().containsKey(Hcl.CODE)) {
                final String latestCode = hclNode.getString(Hcl.CODE);
                final String oldFullLineage = hclNode.getString(Hcl.FULL_LINEAGE);
                String parentFullLineage = StringUtils.substringBeforeLast(oldFullLineage, HclDocHelper.SEPARATOR);
                if (StringUtils.isBlank(parentFullLineage)) {
                    parentFullLineage = StringUtils.EMPTY;
                }
                final String latestFullLineage = parentFullLineage + HclDocHelper.SEPARATOR + latestCode;
                if (!StringUtils.equals(latestFullLineage, oldFullLineage)) {
                    handleHclChildNode(latestFullLineage, oldFullLineage, doc);
                    hclNode.put(Hcl.FULL_LINEAGE, latestFullLineage);

                }
            }
        }

    }

    //get the child hclNodes by originalParentFullLineage, and update these nodes parentFullLineage
    private static void handleHclChildNode(final String latestParentFullLineage,
            final String originalParentFullLineage, final DynamicEntity doc) {
        final List<DynamicEntity> childrenHclNodes = getHclNodesInGroupByParentCode(doc, originalParentFullLineage);
        for (final DynamicEntity node : childrenHclNodes) {
            final String newFullLineage = StringUtils.replaceOnce(node.getString(Hcl.FULL_LINEAGE),
                originalParentFullLineage, latestParentFullLineage);
            node.put(Hcl.FULL_LINEAGE, newFullLineage);
        }
    }
}
