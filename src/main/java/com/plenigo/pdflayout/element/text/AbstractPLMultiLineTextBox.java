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
/**
 * # * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.plenigo.pdflayout.element.text;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.plenigo.pdflayout.base.AbstractPLElement;
import com.plenigo.pdflayout.base.AbstractPLRenderableObject;
import com.plenigo.pdflayout.base.IPLRenderableObject;
import com.plenigo.pdflayout.base.IPLSplittableObject;
import com.plenigo.pdflayout.base.IPLVisitor;
import com.plenigo.pdflayout.base.PLColor;
import com.plenigo.pdflayout.base.PLElementWithSize;
import com.plenigo.pdflayout.base.PLSplitResult;
import com.plenigo.pdflayout.debug.PLDebugLog;
import com.plenigo.pdflayout.element.box.PLBox;
import com.plenigo.pdflayout.element.hbox.PLHBox;
import com.plenigo.pdflayout.element.hbox.PLHBoxColumn;
import com.plenigo.pdflayout.element.link.PLExternalLink;
import com.plenigo.pdflayout.element.vbox.PLVBoxRow;
import com.plenigo.pdflayout.render.PageRenderContext;
import com.plenigo.pdflayout.render.PreparationContext;
import com.plenigo.pdflayout.spec.BorderStyleSpec;
import com.plenigo.pdflayout.spec.EHorzAlignment;
import com.plenigo.pdflayout.spec.FontSpec;
import com.plenigo.pdflayout.spec.HeightSpec;
import com.plenigo.pdflayout.spec.LoadedFont;
import com.plenigo.pdflayout.spec.SizeSpec;
import com.plenigo.pdflayout.spec.TextAndWidthSpec;
import com.plenigo.pdflayout.spec.WidthSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.util.Iterator;

/**
 * Vertical box - groups several rows.
 *
 * @param <IMPLTYPE> Implementation type
 *
 * @author plenigo
 */
public abstract class AbstractPLMultiLineTextBox<IMPLTYPE extends AbstractPLMultiLineTextBox<IMPLTYPE>> extends AbstractPLRenderableObject<IMPLTYPE> implements
        IPLSplittableObject<IMPLTYPE, IMPLTYPE> {
    public static final boolean DEFAULT_FULL_WIDTH = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPLMultiLineTextBox.class);

    // All the rows of this VBox
    private final ICommonsList<PLVBoxRow> m_aRows = new CommonsArrayList<>();
    private final ICommonsList<PLMultiLineText> m_aTextList = new CommonsArrayList<>();
    // Vertical splittable?
    private boolean m_bVertSplittable = DEFAULT_VERT_SPLITTABLE;
    // Header rows to be repeated after a split
    private int m_nHeaderRowCount = 0;
    // Always use the full width?
    private boolean m_bFullWidth = DEFAULT_FULL_WIDTH;
    private EHorzAlignment m_eHorzAlign = EHorzAlignment.DEFAULT;

    // Status vars
    /**
     * prepared row size (with outline of contained element)
     */
    private SizeSpec[] m_aPreparedRowSize;
    /**
     * prepared element size (without outline)
     */
    private SizeSpec[] m_aPreparedElementSize;

    public AbstractPLMultiLineTextBox() {
    }

    @Nonnull
    public final EHorzAlignment getHorzAlign() {
        return m_eHorzAlign;
    }

    @Nonnull
    public final IMPLTYPE setHorzAlign(@Nonnull final EHorzAlignment eHorzAlign) {
        m_eHorzAlign = ValueEnforcer.notNull(eHorzAlign, "HorzAlign");
        return thisAsT();
    }

    @Override
    @Nonnull
    @OverridingMethodsMustInvokeSuper
    public IMPLTYPE setBasicDataFrom(@Nonnull final IMPLTYPE aSource) {
        super.setBasicDataFrom(aSource);
        setVertSplittable(aSource.isVertSplittable());
        setFullWidth(aSource.isFullWidth());
        return thisAsT();
    }

    @Nonnull
    public IMPLTYPE addMultiLineText(@Nonnull final PLMultiLineText aElement) {
        m_aTextList.add(aElement);
        return thisAsT();
    }

    /**
     * @return The number of rows. Always &ge; 0.
     */
    @Nonnegative
    public int getRowCount() {
        return m_aRows.size();
    }

    /**
     * @return All rows. Never <code>null</code>.
     */
    @Nonnull
    public Iterable<PLVBoxRow> getRows() {
        return m_aRows;
    }

    /**
     * Get the row at the specified index.
     *
     * @param nIndex The index to use. Should be &ge; 0.
     *
     * @return <code>null</code> if an invalid index was provided.
     */
    @Nullable
    public PLVBoxRow getRowAtIndex(@Nonnegative final int nIndex) {
        return m_aRows.getAtIndex(nIndex);
    }

    /**
     * Get the element in the row at the specified index.
     *
     * @param nIndex The index to use. Should be &ge; 0.
     *
     * @return <code>null</code> if an invalid index was provided.
     */
    @Nullable
    public IPLRenderableObject<?> getRowElementAtIndex(@Nonnegative final int nIndex) {
        final PLVBoxRow aRow = getRowAtIndex(nIndex);
        return aRow == null ? null : aRow.getElement();
    }

    /**
     * @return The default height to be used for rows if none is provided. May not
     * be <code>null</code>.
     */
    @Nonnull
    public HeightSpec getDefaultHeight() {
        return HeightSpec.auto();
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
        return m_aRows.containsAny(x -> x.getElement().isVertSplittable());
    }

    /**
     * @return Should the VBox occupy the full width? The default is
     * {@link #DEFAULT_FULL_WIDTH}.
     */
    public final boolean isFullWidth() {
        return m_bFullWidth;
    }

    /**
     * Set usage of full width.
     *
     * @param bFullWidth <code>true</code> to enable full width, <code>false</code> to use
     *                   only what is available.
     *
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setFullWidth(final boolean bFullWidth) {
        m_bFullWidth = bFullWidth;
        return thisAsT();
    }

    @Override
    @Nonnull
    public EChange visit(@Nonnull final IPLVisitor aVisitor) throws IOException {
        EChange ret = EChange.UNCHANGED;
        for (final PLVBoxRow aRow : m_aRows)
            ret = ret.or(aRow.getElement().visit(aVisitor));
        return ret;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public SizeSpec onPrepare(@Nonnull final PreparationContext aCtx) {
        final float fElementWidth = aCtx.getAvailableWidth() - getOutlineXSum();
        final float fElementHeight = aCtx.getAvailableHeight() - getOutlineYSum();

        float fRestWidth = fElementWidth;
        float fLastRestWidth = fRestWidth;
        FontHeightSpec fMaxFontHeight = new FontHeightSpec(0, 0);
        PLHBox multiLineTextBox = null;

        // prepare the multiline text and add the real data to the columns.
        for (final PLMultiLineText aText : m_aTextList) {
            // Prepare the max available width if the text is split into lines.
            aText.prepareMaxAvailableWidth(aCtx.getAvailableWidth());
            aText.prepare(new PreparationContext(aCtx.getGlobalContext(), fRestWidth, fElementHeight));
            ICommonsList<TextAndWidthSpec> textLines = aText.getPreperedLines();
            boolean hasNext = false;
            Iterator<TextAndWidthSpec> iterator = null;
            if (textLines != null) {
                iterator = textLines.iterator();
            }
            fLastRestWidth = fRestWidth;
            do {
                String content = "";
                float width = 0;

                if (iterator != null) {
                    if (!iterator.hasNext()) {
                        break;
                    }
                    TextAndWidthSpec element = iterator.next();
                    content = element.getText();
                    width = element.getWidth();
                    hasNext = iterator.hasNext();
                }
                WidthSpec widthSpec = WidthSpec.auto();
                if (width > 0) {
                    widthSpec = WidthSpec.abs(width);
                }
                FontHeightSpec textFontHeight = getFontHeight(aCtx, aText.getFontSpec());
                IPLRenderableObject<?> aElement;

                // check if the text has an uri
                if (aText.hasURI()) {
                    PLColor linkColor = new PLColor(0, 102, 204);
                    PLText text = new PLText(content, aText.getFontSpec().getCloneWithDifferentColor(linkColor)).setMaxRows(1);
                    aElement = new PLExternalLink(text)
                            .setURI(aText.getURI())
                            .setBorderBottom(new BorderStyleSpec(linkColor, 0.5f))
                            .setPaddingBottom(textFontHeight.getDescent() + 0.5f);
                } else {
                    aElement = new PLText(content, aText.getFontSpec()).setMaxRows(1);
                }
                if (multiLineTextBox != null) {
                    fRestWidth = fRestWidth - width;
                    if (fRestWidth <= 0) {
                        if (fLastRestWidth > 0 && getHorzAlign() == EHorzAlignment.RIGHT) {
                            multiLineTextBox.addColumn(0, new PLBox(), WidthSpec.abs(fLastRestWidth));
                        }
                        onPrepareMultiLineTextBox(aCtx, multiLineTextBox, fMaxFontHeight);

                        m_aRows.add(new PLVBoxRow(multiLineTextBox, HeightSpec.auto()));

                        fMaxFontHeight = new FontHeightSpec(0, 0);
                        multiLineTextBox = null;

                        if (width < fElementWidth) {
                            multiLineTextBox = new PLHBox();
                            multiLineTextBox.addColumn(aElement, widthSpec);
                            fMaxFontHeight = textFontHeight;
                            fRestWidth = fElementWidth - width;
                        } else {
                            m_aRows.add(new PLVBoxRow(aElement, HeightSpec.auto()));
                            fRestWidth = fElementWidth;
                        }
                    } else {
                        if (fMaxFontHeight.getFullHeight() < textFontHeight.getFullHeight()) {
                            fMaxFontHeight = textFontHeight;
                        }
                        multiLineTextBox.addColumn(aElement, widthSpec);
                    }

                } else if (hasNext) {
                    m_aRows.add(new PLVBoxRow(aElement, HeightSpec.auto()));
                    fRestWidth = fElementWidth;
                } else if (width < fElementWidth) {
                    multiLineTextBox = new PLHBox();
                    multiLineTextBox.addColumn(aElement, widthSpec);
                    fMaxFontHeight = textFontHeight;
                    fRestWidth = fElementWidth - width;
                }
                fLastRestWidth = fRestWidth;
            } while (hasNext);
        }

        // clean the text list
        m_aTextList.removeAll();

        if (multiLineTextBox != null) {
            if (fLastRestWidth > 0 && getHorzAlign() == EHorzAlignment.RIGHT) {
                multiLineTextBox.addColumn(0, new PLBox(), WidthSpec.abs(fLastRestWidth));
            }
            onPrepareMultiLineTextBox(aCtx, multiLineTextBox, fMaxFontHeight);

            m_aRows.add(new PLVBoxRow(multiLineTextBox, HeightSpec.auto()));
        }

        m_aPreparedRowSize = new SizeSpec[m_aRows.size()];
        m_aPreparedElementSize = new SizeSpec[m_aRows.size()];

        float fMaxRowWidthNet = 0;
        float fMaxRowWidthFull = 0;
        float fUsedHeightFull = 0;

        int nStarRows = 0;
        int nAutoRows = 0;
        for (final PLVBoxRow aRow : m_aRows)
            switch (aRow.getHeight().getType()) {
                case STAR:
                    ++nStarRows;
                    break;
                case AUTO:
                    ++nAutoRows;
                    break;
            }

        int nIndex = 0;
        float fRestHeight = fElementHeight;

        // 1. prepare all absolute height items (absolute or percentage)
        for (final PLVBoxRow aRow : m_aRows) {
            if (aRow.getHeight().isAbsolute()) {
                final IPLRenderableObject<?> aElement = aRow.getElement();

                // Height of this row
                final float fRowHeight = aRow.getHeight().getEffectiveValue(fElementHeight);

                // Prepare child element
                final SizeSpec aElementPreparedSize = aElement.prepare(new PreparationContext(aCtx.getGlobalContext(),
                        fElementWidth,
                        fRowHeight));

                // Update used width
                // Effective content width of this element
                fMaxRowWidthNet = Math.max(fMaxRowWidthNet, aElementPreparedSize.getWidth());
                final float fRowWidthFull = aElementPreparedSize.getWidth() + aElement.getOutlineXSum();
                fMaxRowWidthFull = Math.max(fMaxRowWidthFull, fRowWidthFull);

                // Update used height
                fUsedHeightFull += fRowHeight;
                fRestHeight -= fRowHeight;

                // Without padding and margin
                m_aPreparedRowSize[nIndex] = new SizeSpec(m_bFullWidth ? fElementWidth : fRowWidthFull, fRowHeight);
                m_aPreparedElementSize[nIndex] = aElementPreparedSize;
            }
            ++nIndex;
        }

        // 2. prepare all auto widths items (2-pass)
        float fUsedAutoHeightFullForStar = fUsedHeightFull;
        {
            // First pass: identify all auto columns that directly fit in their
            // available column width

            float fRemainingHeightAuto = 0;
            float fUsedHeightAutoTooHigh = 0;

            // Full width of this element
            final float fAvailableAutoRowHeight = nAutoRows + nStarRows == 0 ? 0 : fRestHeight / (nAutoRows + nStarRows);
            final float fAvailableAutoRowHeightAll = fAvailableAutoRowHeight * nAutoRows;

            final SizeSpec[] aTooHighAutoRows = new SizeSpec[m_aRows.size()];

            nIndex = 0;
            for (final PLVBoxRow aRow : m_aRows) {
                if (aRow.getHeight().isAuto()) {
                    final IPLRenderableObject<?> aElement = aRow.getElement();

                    // Prepare child element
                    final SizeSpec aElementPreparedSize = aElement.prepare(new PreparationContext(aCtx.getGlobalContext(),
                            fElementWidth,
                            fAvailableAutoRowHeightAll));

                    // Use the used size of the element as the row height
                    final float fRowHeightFull = aElementPreparedSize.getHeight() + aElement.getOutlineYSum();

                    if (fRowHeightFull <= fAvailableAutoRowHeight) {
                        // Update used height
                        fUsedHeightFull += fRowHeightFull;
                        fUsedAutoHeightFullForStar += fRowHeightFull;

                        // What's left for other auto rows?
                        fRemainingHeightAuto += fAvailableAutoRowHeight - fRowHeightFull;

                        // Update used width
                        fMaxRowWidthNet = Math.max(fMaxRowWidthNet, aElementPreparedSize.getWidth());
                        final float fRowWidth = aElementPreparedSize.getWidth() + aElement.getOutlineXSum();
                        fMaxRowWidthFull = Math.max(fMaxRowWidthFull, fRowWidth);

                        // Without padding and margin
                        m_aPreparedRowSize[nIndex] = new SizeSpec(m_bFullWidth ? fElementWidth : fRowWidth, fRowHeightFull);
                        m_aPreparedElementSize[nIndex] = aElementPreparedSize;
                    } else {
                        // Remember prepared sized
                        aTooHighAutoRows[nIndex] = aElementPreparedSize;

                        // The whole row height remains
                        fRemainingHeightAuto += fAvailableAutoRowHeight;

                        // What would be used ideally
                        fUsedHeightAutoTooHigh += fRowHeightFull;
                    }
                }
                ++nIndex;
            }

            // Second pass: split all too high auto rows on fRemainingHeightAuto
            nIndex = 0;
            for (final PLVBoxRow aRow : m_aRows) {
                // Only consider too-high auto rows
                if (aRow.getHeight().isAuto() && aTooHighAutoRows[nIndex] != null) {
                    final IPLRenderableObject<?> aElement = aRow.getElement();

                    // Previously prepared size including outline
                    final float fTooHighRowHeight = aTooHighAutoRows[nIndex].getHeight() + aElement.getOutlineYSum();

                    // Percentage of used height compared to total used height of all too
                    // high rows (0-1)
                    final float fAvailableRowHeightPerc = fUsedHeightAutoTooHigh == 0 ? 0 : fTooHighRowHeight / fUsedHeightAutoTooHigh;

                    // Use x% of remaining height
                    // Ensure the height is not smaller than the minimum height - may be
                    // split afterwards
                    final float fNewAvailableRowHeight = Math.max(fRemainingHeightAuto * fAvailableRowHeightPerc,
                            aTooHighAutoRows[nIndex].getHeight());

                    // Prepare child element
                    if (aElement instanceof AbstractPLRenderableObject<?>)
                        ((AbstractPLRenderableObject<?>) aElement).internalMarkAsNotPrepared();
                    final SizeSpec aElementPreparedSize = aElement.prepare(new PreparationContext(aCtx.getGlobalContext(),
                            fElementWidth,
                            fNewAvailableRowHeight));

                    // Use the used size of the element as the row height
                    final float fRowHeightFull = aElementPreparedSize.getHeight() + aElement.getOutlineYSum();

                    // Update used height
                    fMaxRowWidthNet = Math.max(fMaxRowWidthNet, aElementPreparedSize.getWidth());
                    final float fRowWidthFull = aElementPreparedSize.getWidth() + aElement.getOutlineXSum();
                    fMaxRowWidthFull = Math.max(fMaxRowWidthFull, fRowWidthFull);

                    // Update used height
                    fUsedHeightFull += fRowHeightFull;

                    // Note: it may be possible, that the prepared size is higher, in
                    // which case the element may be split afterwards!
                    if (fRowHeightFull > fNewAvailableRowHeight) {
                        fUsedAutoHeightFullForStar += fNewAvailableRowHeight;
                        if (!aElement.isVertSplittable())
                            if (LOGGER.isWarnEnabled())
                                LOGGER.warn("VBox row element " +
                                        aElement.getDebugID() +
                                        " uses more height (" +
                                        fRowHeightFull +
                                        ") than is available (" +
                                        fNewAvailableRowHeight +
                                        " and is NOT vertical splittable!");
                    } else
                        fUsedAutoHeightFullForStar += fRowHeightFull;

                    // Without padding and margin
                    m_aPreparedRowSize[nIndex] = new SizeSpec(m_bFullWidth ? fElementWidth : fRowWidthFull, fRowHeightFull);
                    m_aPreparedElementSize[nIndex] = aElementPreparedSize;

                }
                ++nIndex;
            }

            // remaining unused parts of auto rows is automatically available to
            // star height rows (based on fUsedHeightFull)
        }

        // 3. prepare all star height items
        {
            fRestHeight = fElementHeight - fUsedAutoHeightFullForStar;

            // Rest height may be <= 0 if too many "auto" rows are present that take
            // more than the available height
            final boolean bTooSmallRestHeight = fRestHeight <= 0;
            nIndex = 0;
            for (final PLVBoxRow aRow : m_aRows) {
                if (aRow.getHeight().isStar()) {
                    final IPLRenderableObject<?> aElement = aRow.getElement();

                    // Height of this row
                    if (nStarRows == 0)
                        throw new IllegalStateException("Internal inconsistency");
                    final float fRowHeight = fRestHeight / nStarRows;

                    // Prepare child element
                    // If no height is left, use the full available height
                    final SizeSpec aElementPreparedSize = aElement.prepare(new PreparationContext(aCtx.getGlobalContext(),
                            fElementWidth,
                            bTooSmallRestHeight ? fElementHeight
                                    : fRowHeight));

                    // Update used width
                    // Effective content width of this element
                    fMaxRowWidthNet = Math.max(fMaxRowWidthNet, aElementPreparedSize.getWidth());
                    final float fRowWidthFull = aElementPreparedSize.getWidth() + aElement.getOutlineXSum();
                    fMaxRowWidthFull = Math.max(fMaxRowWidthFull, fRowWidthFull);

                    // Update used height
                    // If no height is left, use the net size of the element
                    final float fRowHeightFull = bTooSmallRestHeight ? aElementPreparedSize.getHeight() + aElement.getOutlineYSum() : fRowHeight;
                    fUsedHeightFull += fRowHeightFull;
                    // Don't change rest-height!

                    // Without padding and margin
                    m_aPreparedRowSize[nIndex] = new SizeSpec(m_bFullWidth ? fElementWidth : fRowWidthFull, fRowHeightFull);
                    m_aPreparedElementSize[nIndex] = aElementPreparedSize;
                }
                ++nIndex;
            }
        }

        // Set min size for block elements
        {
            nIndex = 0;
            for (final PLVBoxRow aRow : m_aRows) {
                final IPLRenderableObject<?> aElement = aRow.getElement();
                if (aElement instanceof AbstractPLElement<?>) {
                    final AbstractPLElement<?> aRealElement = (AbstractPLElement<?>) aElement;
                    // Set minimum row width and height
                    aRealElement.setMinSize(m_bFullWidth ? fElementWidth - aRealElement.getOutlineXSum() : fMaxRowWidthNet,
                            m_aPreparedRowSize[nIndex].getHeight() - aRealElement.getOutlineYSum());
                }
                ++nIndex;
            }
        }

        // Small consistency check (with rounding included)
        if (PLDebugLog.isDebugPrepare()) {
            if (fMaxRowWidthFull - fElementWidth > 0.01)
                PLDebugLog.debugPrepare(this, "uses more width (" + fMaxRowWidthFull + ") than available (" + fElementWidth + ")!");
            if (fUsedHeightFull - fElementHeight > 0.01 && !isVertSplittable())
                PLDebugLog.debugPrepare(this, "uses more height (" + fUsedHeightFull + ") than available (" + fElementHeight + ")!");
        }
        return new SizeSpec(fMaxRowWidthFull, fUsedHeightFull);
    }

    /**
     * On prepare multi line text box.
     *
     * @param aCtx             the a ctx
     * @param multiLineTextBox the multi line text box
     * @param fMaxFontHeight   the f max font height
     */
    public void onPrepareMultiLineTextBox(final PreparationContext aCtx, PLHBox multiLineTextBox, FontHeightSpec fMaxFontHeight) {
        if (multiLineTextBox != null) {
            for (PLHBoxColumn column : multiLineTextBox.getColumns()) {
                PLText columnTextElement = null;
                if ((PLText.class.isAssignableFrom(column.getElement().getClass()))) {
                    columnTextElement = (PLText) column.getElement();
                } else if (PLExternalLink.class.isAssignableFrom(column.getElement().getClass())) {
                    PLExternalLink columnLinkElement = (PLExternalLink) column.getElement();
                    columnTextElement = (PLText) columnLinkElement.getElement();
                }
                if (columnTextElement != null) {
                    FontHeightSpec currentFontHeight = getFontHeight(aCtx, columnTextElement.getFontSpec());
                    if (currentFontHeight.getFullHeight() < fMaxFontHeight.getFullHeight()) {
                        columnTextElement.setMarginTop(fMaxFontHeight.getHeight() - currentFontHeight.getHeight() -
                                (Math.abs(fMaxFontHeight.getDescent()) - Math.abs(currentFontHeight.getDescent())));
                    }
                }
            }
        }
    }

    @Override
    public void onMarkAsNotPrepared() {
        m_aPreparedRowSize = null;
        m_aPreparedElementSize = null;
        for (final PLVBoxRow aRow : m_aRows)
            if (aRow.getElement() instanceof AbstractPLRenderableObject<?>)
                ((AbstractPLRenderableObject<?>) aRow.getElement()).internalMarkAsNotPrepared();
    }

    @Nullable
    public PLSplitResult splitElementVert(final float fAvailableWidth, final float fAvailableHeight) {
        if (fAvailableHeight <= 0)
            return null;

        if (!containsAnyVertSplittableElement()) {
            // Splitting makes no sense
            if (PLDebugLog.isDebugSplit())
                PLDebugLog.debugSplit(this, "Cannot split because no vertical splittable elements are contained");
            return null;
        }

        // Create resulting VBoxes - the first one is not splittable again!
        final AbstractPLMultiLineTextBox<?> aVBox1 = internalCreateNewVertSplitObject(thisAsT()).setID(getID() + "-1").setVertSplittable(false);
        final AbstractPLMultiLineTextBox<?> aVBox2 = internalCreateNewVertSplitObject(thisAsT()).setID(getID() + "-2").setVertSplittable(true);

        final int nTotalRows = getRowCount();
        final ICommonsList<SizeSpec> aVBox1RowSize = new CommonsArrayList<>(nTotalRows);
        final ICommonsList<SizeSpec> aVBox1ElementSize = new CommonsArrayList<>(nTotalRows);
        float fUsedVBox1RowHeight = 0;

        // Copy all header rows to both boxes
        for (int nRow = 0; nRow < m_nHeaderRowCount; ++nRow) {
            final IPLRenderableObject<?> aHeaderRowElement = getRowElementAtIndex(nRow);
            aVBox1.addRow(aHeaderRowElement);
            aVBox2.addRow(aHeaderRowElement);

            fUsedVBox1RowHeight += m_aPreparedRowSize[nRow].getHeight();
            aVBox1RowSize.add(m_aPreparedRowSize[nRow]);
            aVBox1ElementSize.add(m_aPreparedElementSize[nRow]);
        }

        // The height and width after header rows are identical
        final ICommonsList<SizeSpec> aVBox2RowSize = aVBox1RowSize.getClone();
        final ICommonsList<SizeSpec> aVBox2ElementSize = aVBox1ElementSize.getClone();
        float fUsedVBox2RowHeight = fUsedVBox1RowHeight;

        // Copy all content rows
        boolean bOnVBox1 = true;
        for (int nRow = m_nHeaderRowCount; nRow < nTotalRows; ++nRow) {
            final IPLRenderableObject<?> aRowElement = getRowElementAtIndex(nRow);
            final float fRowHeight = m_aPreparedRowSize[nRow].getHeight();

            if (bOnVBox1) {
                if (fUsedVBox1RowHeight + fRowHeight <= fAvailableHeight) {
                    // Row fits in first VBox without a change
                    aVBox1.addRow(aRowElement);
                    fUsedVBox1RowHeight += fRowHeight;
                    // Use data as is
                    aVBox1RowSize.add(m_aPreparedRowSize[nRow]);
                    aVBox1ElementSize.add(m_aPreparedElementSize[nRow]);
                } else {
                    // Row does not fit - check if it can be splitted
                    bOnVBox1 = false;
                    // try to split the row
                    boolean bSplittedRow = false;
                    if (aRowElement.isVertSplittable()) {
                        final float fSplitWidth = m_aPreparedElementSize[nRow].getWidth();
                        final float fSplitHeight = fAvailableHeight - fUsedVBox1RowHeight - aRowElement.getOutlineYSum();
                        if (PLDebugLog.isDebugSplit())
                            PLDebugLog.debugSplit(this,
                                    "Trying to split " +
                                            aRowElement.getDebugID() +
                                            " into pieces for split size " +
                                            PLDebugLog.getWH(fSplitWidth, fSplitHeight));

                        // Try to split the element contained in the row
                        final PLSplitResult aSplitResult = aRowElement.getAsSplittable().splitElementVert(fSplitWidth, fSplitHeight);
                        if (aSplitResult != null) {
                            final IPLRenderableObject<?> aVBox1RowElement = aSplitResult.getFirstElement().getElement();
                            aVBox1.addRow(aVBox1RowElement);
                            fUsedVBox1RowHeight += aSplitResult.getFirstElement().getHeightFull();
                            aVBox1RowSize.add(aSplitResult.getFirstElement().getSizeFull());
                            aVBox1ElementSize.add(aSplitResult.getFirstElement().getSize());

                            final IPLRenderableObject<?> aVBox2RowElement = aSplitResult.getSecondElement().getElement();
                            aVBox2.addRow(aVBox2RowElement);
                            fUsedVBox2RowHeight += aSplitResult.getSecondElement().getHeightFull();
                            aVBox2RowSize.add(aSplitResult.getSecondElement().getSizeFull());
                            aVBox2ElementSize.add(aSplitResult.getSecondElement().getSize());

                            if (PLDebugLog.isDebugSplit())
                                PLDebugLog.debugSplit(this,
                                        "Split row element " +
                                                aRowElement.getDebugID() +
                                                " (Row " +
                                                nRow +
                                                ") into pieces: " +
                                                aVBox1RowElement.getDebugID() +
                                                " (" +
                                                aSplitResult.getFirstElement().getWidth() +
                                                "+" +
                                                aVBox1RowElement.getOutlineXSum() +
                                                " & " +
                                                aSplitResult.getFirstElement().getHeight() +
                                                "+" +
                                                aVBox1RowElement.getOutlineYSum() +
                                                ") and " +
                                                aVBox2RowElement.getDebugID() +
                                                " (" +
                                                aSplitResult.getSecondElement().getWidth() +
                                                "+" +
                                                aVBox2RowElement.getOutlineXSum() +
                                                " & " +
                                                aSplitResult.getSecondElement().getHeight() +
                                                "+" +
                                                aVBox2RowElement.getOutlineYSum() +
                                                ")");
                            bSplittedRow = true;
                        } else {
                            if (PLDebugLog.isDebugSplit())
                                PLDebugLog.debugSplit(this,
                                        "Failed to split row element " + aRowElement.getDebugID() + " (Row " + nRow + ") into pieces");
                        }
                    }

                    if (!bSplittedRow) {
                        // just add the full row to the second VBox since the row does not
                        // fit on first page
                        aVBox2.addRow(aRowElement);
                        fUsedVBox2RowHeight += fRowHeight;
                        aVBox2RowSize.add(m_aPreparedRowSize[nRow]);
                        aVBox2ElementSize.add(m_aPreparedElementSize[nRow]);
                    }
                }
            } else {
                // We're already on VBox 2 - add all elements, since VBox2 may be split
                // again later!
                aVBox2.addRow(aRowElement);
                fUsedVBox2RowHeight += fRowHeight;
                aVBox2RowSize.add(m_aPreparedRowSize[nRow]);
                aVBox2ElementSize.add(m_aPreparedElementSize[nRow]);
            }
        }

        if (aVBox1.getRowCount() == m_nHeaderRowCount) {
            // Splitting makes no sense!
            if (PLDebugLog.isDebugSplit())
                PLDebugLog.debugSplit(this, "Splitting makes no sense, because VBox 1 would be empty");
            return null;
        }

        if (aVBox2.getRowCount() == m_nHeaderRowCount) {
            // Splitting makes no sense!
            if (PLDebugLog.isDebugSplit())
                PLDebugLog.debugSplit(this, "Splitting makes no sense, because VBox 2 would be empty");
            return null;
        }

        // Excluding padding/margin
        aVBox1.internalMarkAsPrepared(new SizeSpec(fAvailableWidth, fUsedVBox1RowHeight));
        aVBox1.m_aPreparedRowSize = ArrayHelper.newArray(aVBox1RowSize, SizeSpec.class);
        aVBox1.m_aPreparedElementSize = ArrayHelper.newArray(aVBox1ElementSize, SizeSpec.class);

        aVBox2.internalMarkAsPrepared(new SizeSpec(fAvailableWidth, fUsedVBox2RowHeight));
        aVBox2.m_aPreparedRowSize = ArrayHelper.newArray(aVBox2RowSize, SizeSpec.class);
        aVBox2.m_aPreparedElementSize = ArrayHelper.newArray(aVBox2ElementSize, SizeSpec.class);

        return new PLSplitResult(new PLElementWithSize(aVBox1, new SizeSpec(fAvailableWidth, fUsedVBox1RowHeight)),
                new PLElementWithSize(aVBox2, new SizeSpec(fAvailableWidth, fUsedVBox2RowHeight)));
    }

    @Override
    protected void onRender(@Nonnull final PageRenderContext aCtx) throws IOException {
        final float fCurX = aCtx.getStartLeft() + getOutlineLeft();
        float fCurY = aCtx.getStartTop() - getOutlineTop();

        int nIndex = 0;
        for (final PLVBoxRow aRow : m_aRows) {
            final IPLRenderableObject<?> aElement = aRow.getElement();
            final float fRowWidth = m_aPreparedRowSize[nIndex].getWidth();
            final float fRowHeight = m_aPreparedRowSize[nIndex].getHeight();

            // Perform contained element after border
            final PageRenderContext aRowElementCtx = new PageRenderContext(aCtx, fCurX, fCurY, fRowWidth, fRowHeight);
            aElement.render(aRowElementCtx);

            // Update Y-pos
            fCurY -= fRowHeight;
            ++nIndex;
        }
    }

    /**
     * Add a row to this VBox using auto height.
     *
     * @param aElement The row to be added. May not be <code>null</code>.
     *
     * @return this
     */
    @Nonnull
    private IMPLTYPE addRow(@Nonnull final IPLRenderableObject<?> aElement) {
        return addRow(aElement, getDefaultHeight());
    }


    /**
     * Add a row to this VBox.
     *
     * @param aElement The row to be added. May not be <code>null</code>.
     * @param aHeight  The height specification to use. May not be <code>null</code>.
     *
     * @return this for chaining
     */
    @Nonnull
    private final IMPLTYPE addRow(@Nonnull final IPLRenderableObject<?> aElement, @Nonnull final HeightSpec aHeight) {
        addAndReturnRow(aElement, aHeight);
        return thisAsT();
    }

    /**
     * Add a row to this VBox.
     *
     * @param aElement The row to be added. May not be <code>null</code>.
     * @param aHeight  The height specification to use. May not be <code>null</code>.
     *
     * @return the created row
     */
    @Nonnull
    private final PLVBoxRow addAndReturnRow(@Nonnull final IPLRenderableObject<?> aElement, @Nonnull final HeightSpec aHeight) {
        internalCheckNotPrepared();
        return _addAndReturnRow(-1, aElement, aHeight);
    }

    /**
     * Add a row to this VBox.
     *
     * @param nIndex   The index where the row should be added. Must be &ge; 0.
     * @param aElement The row to be added. May not be <code>null</code>.
     * @param aHeight  The height specification to use. May not be <code>null</code>.
     *
     * @return the created row. Never <code>null</code>.
     */
    @Nonnull
    private PLVBoxRow addAndReturnRow(@Nonnegative final int nIndex,
                                      @Nonnull final IPLRenderableObject<?> aElement,
                                      @Nonnull final HeightSpec aHeight) {
        ValueEnforcer.isGE0(nIndex, "Index");
        internalCheckNotPrepared();
        return _addAndReturnRow(nIndex, aElement, aHeight);
    }

    @Nonnull
    private PLVBoxRow _addAndReturnRow(@CheckForSigned final int nIndex,
                                       @Nonnull final IPLRenderableObject<?> aElement,
                                       @Nonnull final HeightSpec aHeight) {
        final PLVBoxRow aItem = new PLVBoxRow(aElement, aHeight);
        if (nIndex < 0 || nIndex >= m_aRows.size())
            m_aRows.add(aItem);
        else
            m_aRows.add(nIndex, aItem);
        return aItem;
    }

    /**
     * Instantiates a new Get font height.
     */
    private FontHeightSpec getFontHeight(@Nonnull final PreparationContext aCtx, @Nonnull final FontSpec fontSpec) {
        // Load font into document
        try {
            LoadedFont font = aCtx.getGlobalContext().getLoadedFont(fontSpec);
            float height = font.getTextHeight(fontSpec.getFontSize());
            float descent = font.getDescent(fontSpec.getFontSize());
            return new FontHeightSpec(height, descent);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to prepare text element: " + toString(), ex);
        }
    }


    @Override
    public String toString() {
        return ToStringGenerator.getDerived(super.toString())
                .append("Rows", m_aRows)
                .appendIfNotNull("PreparedRowSize", m_aPreparedRowSize)
                .appendIfNotNull("PreparedElementSize", m_aPreparedElementSize)
                .getToString();
    }

    // Internal class for the font height.
    private class FontHeightSpec {
        private final float height;
        private final float descent;

        public FontHeightSpec(float height, float descent) {
            this.height = height;
            this.descent = descent;
        }

        public float getHeight() {
            return height;
        }

        public float getDescent() {
            return descent;
        }

        public float getFullHeight() {
            return getHeight() + getDescent();
        }
    }
}