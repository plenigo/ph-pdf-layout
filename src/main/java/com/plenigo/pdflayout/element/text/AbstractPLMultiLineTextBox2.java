/*
 * Copyright (C) 2014-2023 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.plenigo.pdflayout.element.text;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.plenigo.pdflayout.base.AbstractPLElement;
import com.plenigo.pdflayout.base.AbstractPLRenderableObject;
import com.plenigo.pdflayout.base.IPLRenderableObject;
import com.plenigo.pdflayout.base.IPLSplittableObject;
import com.plenigo.pdflayout.base.IPLVisitor;
import com.plenigo.pdflayout.base.PLElementWithSize;
import com.plenigo.pdflayout.base.PLSplitResult;
import com.plenigo.pdflayout.debug.PLDebugLog;
import com.plenigo.pdflayout.element.hbox.PLHBox;
import com.plenigo.pdflayout.element.hbox.PLHBoxColumn;
import com.plenigo.pdflayout.element.special.PLSpacerX;
import com.plenigo.pdflayout.element.vbox.PLVBox;
import com.plenigo.pdflayout.render.PageRenderContext;
import com.plenigo.pdflayout.render.PreparationContext;
import com.plenigo.pdflayout.spec.SizeSpec;
import com.plenigo.pdflayout.spec.TextAndWidthSpec;
import com.plenigo.pdflayout.spec.WidthSpec;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.util.Iterator;

/**
 * Horizontal box - groups several columns. Each column was a width with one of
 * the supported types:
 * <ul>
 * <li><b>absolute</b> - the width is explicitly specified in user units</li>
 * <li><b>percentage</b> - the width is specified in percentage of the
 * surrounding element</li>
 * <li><b>star</b> - the width of all columns with this type is evenly spaced on
 * the available width. So if at least one 'star' width column is available, the
 * hbox uses the complete available width.</li>
 * <li><b>auto</b> - the width of the column is determined by the width of the
 * content. The maximum width assigned to this column type is the same as for
 * 'star' width columns.</li>
 * </ul>
 *
 * @param <IMPLTYPE> Implementation type
 *
 * @author Philip Helger
 */
public abstract class AbstractPLMultiLineTextBox2<IMPLTYPE extends AbstractPLMultiLineTextBox2<IMPLTYPE>> extends AbstractPLRenderableObject<IMPLTYPE> implements
        IPLSplittableObject<IMPLTYPE, IMPLTYPE> {
    private final ICommonsList<PLHBoxColumn> m_aColumns = new CommonsArrayList<>();
    private final ICommonsList<PLMultiLineText> m_aTextList = new CommonsArrayList<>();
    private boolean m_bVertSplittable = DEFAULT_VERT_SPLITTABLE;
    private final PLVBox m_aTextBox = new PLVBox();
    /**
     * prepared column size (with outline of contained element)
     */
    private SizeSpec[] m_aPreparedColumnSizes;
    /**
     * prepared element size (without outline)
     */
    private SizeSpec[] m_aPreparedElementSizes;

    public AbstractPLMultiLineTextBox2() {
    }

    @Override
    @Nonnull
    @OverridingMethodsMustInvokeSuper
    public IMPLTYPE setBasicDataFrom(@Nonnull final IMPLTYPE aSource) {
        super.setBasicDataFrom(aSource);
        setVertSplittable(aSource.isVertSplittable());
        return thisAsT();
    }

    /**
     * @return All columns. Never <code>null</code>.
     */
    @Nonnull
    @ReturnsMutableCopy
    public Iterable<PLHBoxColumn> getColumns() {
        return m_aColumns;
    }

    @Nullable
    public PLHBoxColumn getColumnAtIndex(@Nonnegative final int nIndex) {
        return m_aColumns.getAtIndex(nIndex);
    }

    @Nullable
    public IPLRenderableObject<?> getColumnElementAtIndex(@Nonnegative final int nIndex) {
        final PLHBoxColumn aColumn = getColumnAtIndex(nIndex);
        return aColumn == null ? null : aColumn.getElement();
    }

    private void _addAndReturnColumn(@CheckForSigned final int nIndex, @Nonnull final PLHBoxColumn aColumn) {
        internalCheckNotPrepared();
        if (nIndex < 0 || nIndex >= m_aColumns.size())
            m_aColumns.add(aColumn);
        else
            m_aColumns.add(nIndex, aColumn);
    }

    @Nonnull
    public IMPLTYPE addColumn(@Nonnull final IPLRenderableObject<?> aElement, @Nonnull final WidthSpec aWidth) {
        addAndReturnColumn(-1, aElement, aWidth);
        return thisAsT();
    }

    @Nonnull
    public PLHBoxColumn addAndReturnColumn(@CheckForSigned final int nIndex,
            @Nonnull final IPLRenderableObject<?> aElement,
            @Nonnull final WidthSpec aWidth) {
        final PLHBoxColumn aColumn = new PLHBoxColumn(aElement, aWidth);
        _addAndReturnColumn(nIndex, aColumn);
        return aColumn;
    }

    @Nonnull
    public IMPLTYPE addColumn(@CheckForSigned final int nIndex,
            @Nonnull final IPLRenderableObject<?> aElement,
            @Nonnull final WidthSpec aWidth) {
        addAndReturnColumn(nIndex, aElement, aWidth);
        return thisAsT();
    }

    @Nonnull
    public IMPLTYPE addMultiLineText(@Nonnull final PLMultiLineText aElement) {
        m_aTextList.add(aElement);
        return thisAsT();
    }

    public final boolean isVertSplittable() {
        return m_bVertSplittable;
    }

    @Nonnull
    public final IMPLTYPE setVertSplittable(final boolean bVertSplittable) {
        m_bVertSplittable = bVertSplittable;
        return thisAsT();
    }

    public boolean containsAnyVertSplittableElement() {
        return m_aColumns.containsAny(x -> x.getElement().isVertSplittable());
    }

    @Override
    @Nonnull
    public EChange visit(@Nonnull final IPLVisitor aVisitor) throws IOException {
        EChange ret = EChange.UNCHANGED;
        for (final PLMultiLineText aColumn : m_aTextList)
            ret = ret.or(aColumn.visit(aVisitor));
        return ret;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected SizeSpec onPrepare(@Nonnull final PreparationContext aCtx) {
        m_aPreparedColumnSizes = new SizeSpec[m_aColumns.size()];
        m_aPreparedElementSizes = new SizeSpec[m_aColumns.size()];

        final float fElementWidth = aCtx.getAvailableWidth() - getOutlineXSum();
        final float fElementHeight = aCtx.getAvailableHeight() - getOutlineYSum();
        float fUsedWidthFull = 0;
        float fMaxColumnHeightFull = 0;
        float fMaxContentHeightNet = 0;

        int nStarColumns = 0;
        int nAutoColumns = 0;
        for (final PLHBoxColumn aColumn : m_aColumns)
            switch (aColumn.getWidth().getType()) {
                case STAR:
                    ++nStarColumns;
                    break;
                case AUTO:
                    ++nAutoColumns;
                    break;
            }

        int nIndex = 0;
        float fRestWidth = fElementWidth;
        // 1. prepare all width items
        {
            PLHBox multiLineTextBox = null;
            for (final PLMultiLineText aText : m_aTextList) {
                // Prepare child element
                final SizeSpec aElementPreparedSize = aText.prepare(new PreparationContext(aCtx.getGlobalContext(),
                        fRestWidth,
                        fElementHeight));

                ICommonsList<TextAndWidthSpec> textLines = aText.getPreperedLines();
                Iterator<TextAndWidthSpec> it = textLines.iterator();
                while (it.hasNext()) {
                    fRestWidth = fElementWidth;
                    TextAndWidthSpec element = it.next();
                    PLText text = new PLText(element.getText(), aText.getFontSpec());
                    if (multiLineTextBox != null) {
                        multiLineTextBox.addColumn(text, WidthSpec.auto());
                        m_aTextBox.addRow(multiLineTextBox);
                        multiLineTextBox = null;
                    } else if (it.hasNext()) {
                        m_aTextBox.addRow(text);
                    } else if (element.getWidth() < fElementWidth) {
                        multiLineTextBox = new PLHBox();
                        multiLineTextBox.addColumn(text, WidthSpec.auto());
                        fRestWidth = fElementWidth - element.getWidth();
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onMarkAsNotPrepared() {
        m_aPreparedColumnSizes = null;
        m_aPreparedElementSizes = null;
    }

    /**
     * Create an empty element that is to be used as a place holder for splitting.
     * The returned object must be prepared!
     *
     * @param aSrcObject Source element of the hbox to be split
     * @param fWidth     Column width
     * @param fHeight    Column height
     *
     * @return Never <code>null</code>.
     */
    @Nonnull
    protected AbstractPLRenderableObject<?> internalCreateVertSplitEmptyElement(@Nonnull final IPLRenderableObject<?> aSrcObject,
            final float fWidth,
            final float fHeight) {
        return PLSpacerX.createPrepared(fWidth, 0);
    }

    @Nullable
    public PLSplitResult splitElementVert(final float fAvailableWidth, final float fAvailableHeight) {
        if (fAvailableHeight <= 0)
            return null;

        if (!containsAnyVertSplittableElement()) {
            // Splitting makes no sense
            if (PLDebugLog.isDebugSplit())
                PLDebugLog.debugSplit(this, "cannot split because no vertical splittable elements are contained");
            return null;
        }

        final int nCols = m_aColumns.size();

        // Check if height is exceeded
        {
            boolean bAnySplittingPossibleAndNecessary = false;
            for (int i = 0; i < nCols; ++i) {
                // Is the current element higher and splittable?
                final IPLRenderableObject<?> aColumnElement = getColumnElementAtIndex(i);
                if (aColumnElement.isVertSplittable()) {
                    final float fColumnHeightFull = m_aPreparedColumnSizes[i].getHeight();
                    if (fColumnHeightFull > fAvailableHeight) {
                        bAnySplittingPossibleAndNecessary = true;
                        break;
                    }
                }
            }

            if (!bAnySplittingPossibleAndNecessary) {
                // Splitting makes no sense
                if (PLDebugLog.isDebugSplit())
                    PLDebugLog.debugSplit(this,
                            "no need to split because all splittable elements easily fit into the available height (" +
                                    fAvailableHeight +
                                    ")");
                return null;
            }
        }

        final AbstractPLMultiLineTextBox2<?> aHBox1 = internalCreateNewVertSplitObject(thisAsT()).setID(getID() + "-1").setVertSplittable(false);
        final AbstractPLMultiLineTextBox2<?> aHBox2 = internalCreateNewVertSplitObject(thisAsT()).setID(getID() + "-2").setVertSplittable(true);

        // Fill all columns with empty content
        for (int i = 0; i < nCols; ++i) {
            final PLHBoxColumn aColumn = getColumnAtIndex(i);
            final WidthSpec aColumnWidth = aColumn.getWidth();
            final float fColumnWidth = m_aPreparedColumnSizes[i].getWidth();
            final float fColumnHeight = fAvailableHeight;

            // Create empty element with the same width as the original element
            final IPLRenderableObject<?> aSrcElement = aColumn.getElement();
            aHBox1.addColumn(internalCreateVertSplitEmptyElement(aSrcElement, fColumnWidth, fColumnHeight).setID(aSrcElement.getID() + "-1"),
                    aColumnWidth);
            aHBox2.addColumn(internalCreateVertSplitEmptyElement(aSrcElement, fColumnWidth, fColumnHeight).setID(aSrcElement.getID() + "-2"),
                    aColumnWidth);
        }

        float fHBox1MaxHeightNet = 0;
        float fHBox2MaxHeightNet = 0;
        float fHBox1MaxHeightFull = 0;
        float fHBox2MaxHeightFull = 0;
        final SizeSpec[] aHBox1ColumnSizes = new SizeSpec[m_aPreparedColumnSizes.length];
        final SizeSpec[] aHBox2ColumnSizes = new SizeSpec[m_aPreparedColumnSizes.length];
        final SizeSpec[] aHBox1ElementSizes = new SizeSpec[m_aPreparedElementSizes.length];
        final SizeSpec[] aHBox2ElementSizes = new SizeSpec[m_aPreparedElementSizes.length];

        // Start splitting columns
        boolean bDidSplitAnyColumn = false;
        for (int nCol = 0; nCol < nCols; nCol++) {
            final IPLRenderableObject<?> aColumnElement = getColumnElementAtIndex(nCol);
            final boolean bIsSplittable = aColumnElement.isVertSplittable();
            final float fColumnWidth = m_aPreparedColumnSizes[nCol].getWidth();
            final float fColumnHeight = m_aPreparedColumnSizes[nCol].getHeight();
            final float fElementWidthNet = m_aPreparedElementSizes[nCol].getWidth();

            boolean bDidSplitColumn = false;
            if (fColumnHeight > fAvailableHeight && bIsSplittable) {
                final float fSplitWidth = fElementWidthNet;
                final float fSplitHeight = fAvailableHeight - aColumnElement.getOutlineYSum();
                if (PLDebugLog.isDebugSplit())
                    PLDebugLog.debugSplit(this,
                            "Trying to split " +
                                    aColumnElement.getDebugID() +
                                    " with height " +
                                    fColumnHeight +
                                    " into pieces for remaining size " +
                                    PLDebugLog.getWH(fSplitWidth, fSplitHeight));

                // Use width and height without padding and margin!
                final PLSplitResult aSplitResult = aColumnElement.getAsSplittable().splitElementVert(fSplitWidth, fSplitHeight);
                if (aSplitResult != null) {
                    final IPLRenderableObject<?> aHBox1Element = aSplitResult.getFirstElement().getElement();


                    final IPLRenderableObject<?> aHBox2Element = aSplitResult.getSecondElement().getElement();


                    // Use the full height, because the column itself has no padding or
                    // margin!
                    aHBox1ColumnSizes[nCol] = new SizeSpec(fColumnWidth, aSplitResult.getFirstElement().getHeightFull());
                    aHBox2ColumnSizes[nCol] = new SizeSpec(fColumnWidth, aSplitResult.getSecondElement().getHeightFull());
                    aHBox1ElementSizes[nCol] = new SizeSpec(fElementWidthNet, aSplitResult.getFirstElement().getHeight());
                    aHBox2ElementSizes[nCol] = new SizeSpec(fElementWidthNet, aSplitResult.getSecondElement().getHeight());
                    bDidSplitColumn = true;
                    bDidSplitAnyColumn = true;

                    if (PLDebugLog.isDebugSplit())
                        PLDebugLog.debugSplit(this,
                                "Split column element " +
                                        aColumnElement.getDebugID() +
                                        " (Column " +
                                        nCol +
                                        ") into pieces: " +
                                        aHBox1Element.getDebugID() +
                                        " (" +
                                        aSplitResult.getFirstElement().getWidth() +
                                        "+" +
                                        aHBox1Element.getOutlineXSum() +
                                        " & " +
                                        aSplitResult.getFirstElement().getHeight() +
                                        "+" +
                                        aHBox1Element.getOutlineYSum() +
                                        ") and " +
                                        aHBox2Element.getDebugID() +
                                        " (" +
                                        aSplitResult.getSecondElement().getWidth() +
                                        "+" +
                                        aHBox2Element.getOutlineXSum() +
                                        " & " +
                                        aSplitResult.getSecondElement().getHeight() +
                                        "+" +
                                        aHBox2Element.getOutlineYSum() +
                                        ") for available height " +
                                        fAvailableHeight);
                } else {
                    if (PLDebugLog.isDebugSplit())
                        PLDebugLog.debugSplit(this,
                                "Failed to split column element " +
                                        aColumnElement.getDebugID() +
                                        " (Column " +
                                        nCol +
                                        ") with height " +
                                        fColumnHeight +
                                        " into pieces for available height " +
                                        fAvailableHeight);
                }
            }

            if (!bDidSplitColumn) {
                // No splitting and cell fits totally in available height

                // Use "as-is sizes" and not render sizes
                aHBox1ColumnSizes[nCol] = new SizeSpec(fColumnWidth, aColumnElement.getPreparedHeight() + aColumnElement.getOutlineYSum());
                aHBox2ColumnSizes[nCol] = new SizeSpec(fColumnWidth, 0);
                aHBox1ElementSizes[nCol] = new SizeSpec(fElementWidthNet, aColumnElement.getPreparedHeight());
                aHBox2ElementSizes[nCol] = new SizeSpec(fElementWidthNet, 0);
            }

            // calculate max column height
            fHBox1MaxHeightNet = Math.max(fHBox1MaxHeightNet, aHBox1ElementSizes[nCol].getHeight());
            fHBox2MaxHeightNet = Math.max(fHBox2MaxHeightNet, aHBox2ElementSizes[nCol].getHeight());
            fHBox1MaxHeightFull = Math.max(fHBox1MaxHeightFull, aHBox1ColumnSizes[nCol].getHeight());
            fHBox2MaxHeightFull = Math.max(fHBox2MaxHeightFull, aHBox2ColumnSizes[nCol].getHeight());
        }

        if (!bDidSplitAnyColumn) {
            // Nothing was splitted
            if (PLDebugLog.isDebugSplit())
                PLDebugLog.debugSplit(this, "Weird: No column was split and the height is OK!");
            return null;
        }

        // Set min size for block elements
        {
            for (int nIndex = 0; nIndex < m_aColumns.size(); ++nIndex) {
                final IPLRenderableObject<?> aElement1 = aHBox1.getColumnElementAtIndex(nIndex);
                if (aElement1 instanceof AbstractPLElement<?>) {
                    // Set minimum column width and height as prepared width
                    final AbstractPLElement<?> aRealElement1 = (AbstractPLElement<?>) aElement1;
                    aRealElement1.setMinSize(m_aPreparedColumnSizes[nIndex].getWidth() - aRealElement1.getOutlineXSum(), fHBox1MaxHeightNet);
                }

                final IPLRenderableObject<?> aElement2 = aHBox2.getColumnElementAtIndex(nIndex);
                if (aElement2 instanceof AbstractPLElement<?>) {
                    // Set minimum column width and height as prepared width
                    final AbstractPLElement<?> aRealElement2 = (AbstractPLElement<?>) aElement2;
                    aRealElement2.setMinSize(m_aPreparedColumnSizes[nIndex].getWidth() - aRealElement2.getOutlineXSum(), fHBox2MaxHeightNet);
                }
            }
        }

        // mark new hboxes as prepared
        aHBox1.internalMarkAsPrepared(new SizeSpec(fAvailableWidth, fHBox1MaxHeightFull));
        aHBox2.internalMarkAsPrepared(new SizeSpec(fAvailableWidth, fHBox2MaxHeightFull));
        // set prepared column sizes
        aHBox1.m_aPreparedColumnSizes = aHBox1ColumnSizes;
        aHBox2.m_aPreparedColumnSizes = aHBox2ColumnSizes;
        // set prepared element sizes
        aHBox1.m_aPreparedElementSizes = aHBox1ElementSizes;
        aHBox2.m_aPreparedElementSizes = aHBox2ElementSizes;

        return new PLSplitResult(new PLElementWithSize(aHBox1, new SizeSpec(fAvailableWidth, fHBox1MaxHeightFull)),
                new PLElementWithSize(aHBox2, new SizeSpec(fAvailableWidth, fHBox2MaxHeightFull)));
    }

    @Override
    protected void onRender(@Nonnull final PageRenderContext aCtx) throws IOException {
        float fCurX = aCtx.getStartLeft();
        final float fStartY = aCtx.getStartTop();

        int nIndex = 0;
        for (final PLHBoxColumn aColumn : m_aColumns) {
            final IPLRenderableObject<?> aElement = aColumn.getElement();
            final float fColumnWidth = m_aPreparedColumnSizes[nIndex].getWidth();
            final float fColumnHeight = m_aPreparedColumnSizes[nIndex].getHeight();

            final PageRenderContext aItemCtx = new PageRenderContext(aCtx, fCurX, fStartY, fColumnWidth, fColumnHeight);
            aElement.render(aItemCtx);

            // Update X-pos
            fCurX += fColumnWidth;
            ++nIndex;
        }
    }

    @Override
    public String toString() {
        return ToStringGenerator.getDerived(super.toString())
                .append("Columns", m_aColumns)
                .append("VertSplittable", m_bVertSplittable)
                .appendIfNotNull("PreparedColumnSize", m_aPreparedColumnSizes)
                .appendIfNotNull("PreparedElementSize", m_aPreparedElementSizes)
                .getToString();
    }
}
