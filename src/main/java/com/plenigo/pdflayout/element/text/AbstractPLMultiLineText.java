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

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.plenigo.pdflayout.base.AbstractPLRenderableObject;
import com.plenigo.pdflayout.base.EPLPlaceholder;
import com.plenigo.pdflayout.base.IPLHasHorizontalAlignment;
import com.plenigo.pdflayout.debug.PLDebugLog;
import com.plenigo.pdflayout.pdfbox.PDPageContentStreamWithCache;
import com.plenigo.pdflayout.render.PagePreRenderContext;
import com.plenigo.pdflayout.render.PageRenderContext;
import com.plenigo.pdflayout.render.PreparationContext;
import com.plenigo.pdflayout.spec.EHorzAlignment;
import com.plenigo.pdflayout.spec.FontSpec;
import com.plenigo.pdflayout.spec.LoadedFont;
import com.plenigo.pdflayout.spec.SizeSpec;
import com.plenigo.pdflayout.spec.TextAndWidthSpec;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Render text to multi lines.
 *
 * @param <IMPLTYPE> Implementation type
 *
 * @author plenigo
 */
public abstract class AbstractPLMultiLineText<IMPLTYPE extends AbstractPLMultiLineText<IMPLTYPE>> extends AbstractPLRenderableObject<IMPLTYPE> implements
        IPLHasHorizontalAlignment<IMPLTYPE> {
    public static final float DEFAULT_LINE_SPACING = 1f;
    public static final int DEFAULT_MAX_ROWS = CGlobal.ILLEGAL_UINT;

    private String m_sOriginalText;
    private String m_sTextWithPlaceholdersReplaced;
    private String m_sURI;
    private final FontSpec m_aFontSpec;
    private float m_fLineSpacing = DEFAULT_LINE_SPACING;
    private float m_availableRenderWidth;

    private EHorzAlignment m_eHorzAlign = DEFAULT_HORZ_ALIGNMENT;
    private int m_nMaxRows = DEFAULT_MAX_ROWS;

    // prepare result
    private transient LoadedFont m_aLoadedFont;
    protected float m_fTextHeight;
    protected float m_fDescent;
    private float m_fMaxAvailableWidth = 0f;
    protected int m_nPreparedLineCountUnmodified = CGlobal.ILLEGAL_UINT;
    protected ICommonsList<TextAndWidthSpec> m_aPreparedLinesUnmodified;
    protected ICommonsList<TextAndWidthSpec> m_aPreparedLines;

    @Nonnull
    public static String getCleanedPLText(@Nullable final String sText) {
        if (StringHelper.hasNoText(sText)) {
            return "";
        }
        // Unify line endings so that all "\r" are removed and only "\n" is
        // contained
        // Multiple \n after each other remain
        String sCleaned = sText;
        sCleaned = StringHelper.replaceAll(sCleaned, "\r\n", "\n");
        sCleaned = StringHelper.replaceAll(sCleaned, '\r', '\n');
        return sCleaned;
    }

    public AbstractPLMultiLineText(@Nullable final String sText, @Nonnull final FontSpec aFontSpec) {
        _setText(sText);
        m_aFontSpec = ValueEnforcer.notNull(aFontSpec, "FontSpec");
    }

    /**
     * Set the internal text fields
     *
     * @param sText Text to use. May be <code>null</code>.
     */
    private void _setText(@Nullable final String sText) {
        m_sOriginalText = getCleanedPLText(sText);
        m_sTextWithPlaceholdersReplaced = m_sOriginalText;
    }

    @Override
    @Nonnull
    @OverridingMethodsMustInvokeSuper
    public IMPLTYPE setBasicDataFrom(@Nonnull final IMPLTYPE aSource) {
        super.setBasicDataFrom(aSource);
        setHorzAlign(aSource.getHorzAlign());
        setMaxRows(aSource.getMaxRows());
        return thisAsT();
    }

    /**
     * @return The original text provided in the constructor, with newlines
     * unified. Never <code>null</code>.
     */
    @Nonnull
    public final String getText() {
        return m_sOriginalText;
    }

    /**
     * @return <code>true</code> if the contained text has at least one character,
     * <code>false</code> if it is empty.
     */
    public final boolean hasText() {
        return m_sOriginalText.length() > 0;
    }

    /**
     * @return <code>true</code> if the text provided in the constructor contains
     * no character, <code>false</code> otherwise.
     */
    public final boolean hasNoText() {
        return m_sOriginalText.length() == 0;
    }

    /**
     * @return The URI to link to. May be <code>null</code>.
     */
    @Nullable
    public final String getURI() {
        return m_sURI;
    }

    /**
     * Set the URI to link to.
     *
     * @param sURI The URI to link to. May be <code>null</code>.
     *
     * @return this for chaining.
     */
    @Nonnull
    public final IMPLTYPE setURI(@Nullable final String sURI) {
        m_sURI = sURI;
        return thisAsT();
    }

    /**
     * @return <code>true</code> if the contained URI has at least one character,
     * <code>false</code> if it is empty.
     */
    public final boolean hasURI() {
        return m_sURI != null && m_sURI.length() > 0;
    }

    /**
     * @return The font specification to be used as provided in the constructor.
     * Never <code>null</code>.
     */
    @Nonnull
    public final FontSpec getFontSpec() {
        return m_aFontSpec;
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

    /**
     * @return The maximum number of rows to be rendered. If this value is &le; 0
     * than all rows are rendered. The default value is
     * {@link #DEFAULT_MAX_ROWS} meaning all rows are rendered.
     */
    @CheckForSigned
    public final int getMaxRows() {
        return m_nMaxRows;
    }

    /**
     * Set the maximum number of rows to render.
     *
     * @param nMaxRows Maximum number of rows. If &le; 0 than all lines are rendered.
     *
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setMaxRows(final int nMaxRows) {
        m_nMaxRows = nMaxRows;
        return thisAsT();
    }

    final void internalSetPreparedLines(@Nonnull final ICommonsList<TextAndWidthSpec> aLines) {
        final int nLineCount = aLines.size();
        m_nPreparedLineCountUnmodified = nLineCount;
        m_aPreparedLinesUnmodified = aLines;
        if (m_nMaxRows <= 0) {
            // Use all lines
            m_aPreparedLines = aLines;
        } else {
            // Use only a certain maximum number of rows
            if (nLineCount <= m_nMaxRows) {
                // We have less lines than the maximum
                m_aPreparedLines = aLines;
            } else {
                // Maximum number of lines exceeded - copy only the relevant lines
                m_aPreparedLines = new CommonsArrayList<>(m_nMaxRows);
                for (int i = 0; i < m_nMaxRows; ++i)
                    m_aPreparedLines.add(aLines.get(i));
            }
        }
    }

    // Call only once here - used read-only!
    private static final ICommonsMap<String, String> ESTIMATION_REPLACEMENTS = EPLPlaceholder.getEstimationReplacements();

    /**
     * Prepare max available width.
     *
     * @param fMaxAvailableWidth the max available width
     */
    protected void prepareMaxAvailableWidth(float fMaxAvailableWidth) {
        this.m_fMaxAvailableWidth = fMaxAvailableWidth;
    }

    /**
     * This method can only be called after loadedFont member was set!
     *
     * @param fAvailableWidth  Available with
     * @param bAlreadyReplaced <code>true</code> if the text was already replaced
     *
     * @return The new preparation size
     *
     * @throws IOException On PDFBox error
     */
    @Nonnull
    private SizeSpec _prepareText(final float fAvailableWidth, final boolean bAlreadyReplaced) throws IOException {
        final float fFontSize = m_aFontSpec.getFontSize();
        m_fTextHeight = m_aLoadedFont.getTextHeight(fFontSize);
        m_fDescent = m_aLoadedFont.getDescent(fFontSize);

        if (m_fMaxAvailableWidth == 0) {
            m_fMaxAvailableWidth = fAvailableWidth;
        }
        if (m_availableRenderWidth == 0) {
            m_availableRenderWidth = fAvailableWidth;
        }
        if (hasNoText()) {
            // Nothing to do - empty
            // But keep the height distance!
            return new SizeSpec(0, m_fTextHeight);
        }

        // Split text into rows
        final String sTextToFit;
        if (bAlreadyReplaced) {
            sTextToFit = m_sTextWithPlaceholdersReplaced;
        } else {
            // Use the approximations from the placeholders
            sTextToFit = StringHelper.replaceMultiple(m_sOriginalText, ESTIMATION_REPLACEMENTS);
        }
        internalSetPreparedLines(m_aLoadedFont.getFitToWidth(sTextToFit, fFontSize, fAvailableWidth, m_fMaxAvailableWidth));

        // Determine max width of all prepared lines
        float fMaxWidth = Float.MIN_VALUE;
        if (m_aPreparedLines.getLastOrNull() != null)
            fMaxWidth = Math.max(fMaxWidth, m_aPreparedLines.getLastOrNull().getWidth());

        // Determine height by number of lines
        // No line spacing for the last line
        return new SizeSpec(fMaxWidth, getDisplayHeightOfLineCount(m_aPreparedLines.size(), false));
    }

    @Override
    protected SizeSpec onPrepare(@Nonnull final PreparationContext aCtx) {
        final float fElementWidth = aCtx.getAvailableWidth() - getOutlineXSum();

        // Load font into document
        try {
            m_aLoadedFont = aCtx.getGlobalContext().getLoadedFont(m_aFontSpec);
            return _prepareText(fElementWidth, false);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to prepare text element: " + toString(), ex);
        }
    }

    @Override
    protected void onMarkAsNotPrepared() {
        m_nPreparedLineCountUnmodified = CGlobal.ILLEGAL_UINT;
        m_aPreparedLinesUnmodified = null;
        m_aPreparedLines = null;
    }

    protected final float getDisplayHeightOfLineCount(@Nonnegative final int nLineCount, final boolean bLineSpacingAlsoOnLastLine) {
        if (nLineCount == 0)
            return 0f;
        if (nLineCount == 1)
            return m_fTextHeight;

        if (bLineSpacingAlsoOnLastLine)
            return nLineCount * m_fTextHeight * m_fLineSpacing;

        // The line height factor counts only between lines!
        return (nLineCount - 1) * m_fTextHeight * m_fLineSpacing + 1 * m_fTextHeight;
    }

    @Override
    @Nonnull
    public EChange beforeRender(@Nonnull final PagePreRenderContext aCtx) throws IOException {
        return EChange.UNCHANGED;
    }

    protected ICommonsList<TextAndWidthSpec> getPreperedLines() {
        return m_aPreparedLines;
    }

    @Override
    protected void onRender(@Nonnull final PageRenderContext aCtx) throws IOException {
        if (hasNoText()) {
            // Nothing to do - empty text
            return;
        }

        final float fRenderLeft = aCtx.getStartLeft() + getOutlineLeft();
        final float fRenderTop = aCtx.getStartTop() - getOutlineTop();

        if (PLDebugLog.isDebugRender())
            PLDebugLog.debugRender(this,
                    "Display at " +
                            PLDebugLog.getXYWH(fRenderLeft, fRenderTop, getRenderWidth(), getRenderHeight()) +
                            " with " +
                            m_aPreparedLines.size() +
                            " lines");

        final PDPageContentStreamWithCache aContentStream = aCtx.getContentStream();

        aContentStream.beginText();

        // Set font if changed
        aContentStream.setFont(m_aLoadedFont, m_aFontSpec);

        final float fTextHeight = m_fTextHeight;
        final float fPreparedWidth = getPreparedWidth();
        final boolean bDoJustifyText = m_eHorzAlign == EHorzAlignment.JUSTIFY;

        int nIndex = 0;
        final int nMax = m_aPreparedLines.size();
        for (final TextAndWidthSpec aTW : m_aPreparedLines) {
            final boolean bBeforeLastLine = nIndex < (nMax - 1);

            // Replace text (if any)
            final float fTextWidth = aTW.getWidth();
            final String sDrawText = aTW.getText();

            // Align text line by overall block width
            final float fIndentX = getIndentX(fPreparedWidth, fTextWidth);
            if (nIndex == 0) {
                // Initial move - only partial line height!
                aContentStream.moveTextPositionByAmount(fRenderLeft + fIndentX, fRenderTop - fTextHeight - m_fDescent);
            } else if (nIndex == 1) {
                aContentStream.moveTextPositionByAmount(fRenderLeft * -1, 0);
            } else if (fIndentX != 0) {
                // Indent subsequent line
                aContentStream.moveTextPositionByAmount(fIndentX, 0);
            }

            if (bDoJustifyText) {
                if (bBeforeLastLine) {
                    // Avoid division by zero
                    float fCharSpacing = 0;
                    if (sDrawText.length() > 1) {
                        // Calculate width of space between each character (therefore -1)
                        fCharSpacing = (fPreparedWidth - fTextWidth) / (sDrawText.length() - 1);
                    }

                    // Set for each line separately,
                    aContentStream.setCharacterSpacing(fCharSpacing);
                } else {
                    // On last line, no justify
                    // Important to reset back to default after all (if any was set)
                    if (nIndex > 0)
                        aContentStream.setCharacterSpacing(0);
                }
            }

            // Main draw string
            aContentStream.drawString(sDrawText);
            ++nIndex;

            // Goto next line
            // Handle indent per-line as when right alignment is used, the indentX may
            // differ from line to line
            if (bBeforeLastLine) {
                // Outdent and one line down, except for last line
                aContentStream.moveTextPositionByAmount(-fIndentX, -fTextHeight * m_fLineSpacing);
            }
        }

        aContentStream.endText();
    }

    @Override
    public String toString() {
        return ToStringGenerator.getDerived(super.toString())
                .append("OriginalText", m_sOriginalText)
                .append("TextWithPlaceholdersReplaced", m_sTextWithPlaceholdersReplaced)
                .append("FontSpec", m_aFontSpec)
                .append("LineSpacing", m_fLineSpacing)
                .append("HorzAlign", m_eHorzAlign)
                .append("MaxRows", m_nMaxRows)
                .getToString();
    }
}
