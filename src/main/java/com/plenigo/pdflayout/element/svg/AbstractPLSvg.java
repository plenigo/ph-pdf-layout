package com.plenigo.pdflayout.element.svg;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.kitfox.svg.SVGException;
import com.plenigo.pdflayout.base.AbstractPLInlineElement;
import com.plenigo.pdflayout.pdfbox.PDPageContentStreamWithCache;
import com.plenigo.pdflayout.render.PLRenderHelper;
import com.plenigo.pdflayout.render.PagePreRenderContext;
import com.plenigo.pdflayout.render.PageRenderContext;
import com.plenigo.pdflayout.render.PreparationContext;
import com.plenigo.pdflayout.spec.SizeSpec;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Base class for a static svg images.
 *
 * @param <IMPLTYPE> Implementation type
 *
 * @author plenigo
 */
public abstract class AbstractPLSvg<IMPLTYPE extends AbstractPLSvg<IMPLTYPE>> extends AbstractPLInlineElement<IMPLTYPE> {

    private final float m_fImageWidth;
    private final float m_fImageHeight;

    // Status var
    private PDFormXObject m_aXObject;

    public AbstractPLSvg(@Nonnegative final float fImageWidth, @Nonnegative final float fImageHeight) {
        ValueEnforcer.isGT0(fImageWidth, "ImageWidth");
        ValueEnforcer.isGT0(fImageHeight, "ImageHeight");

        m_fImageWidth = fImageWidth;
        m_fImageHeight = fImageHeight;
    }

    @Override
    @Nonnull
    @OverridingMethodsMustInvokeSuper
    public IMPLTYPE setBasicDataFrom(@Nonnull final IMPLTYPE aSource) {
        super.setBasicDataFrom(aSource);
        return thisAsT();
    }

    @Nonnegative
    public final float getImageWidth() {
        return m_fImageWidth;
    }

    @Nonnegative
    public final float getImageHeight() {
        return m_fImageHeight;
    }

    @Override
    protected SizeSpec onPrepare(@Nonnull final PreparationContext aCtx) {
        return new SizeSpec(m_fImageWidth, m_fImageHeight);
    }

    @Override
    protected void onMarkAsNotPrepared() {
        // Nada
    }

    /**
     * Resolve the {@link PDFormXObject} for rendering.
     *
     * @param aCtx Render context
     *
     * @return Never <code>null</code>.
     *
     * @throws IOException In case of error.
     */
    @Nonnull
    protected abstract PDFormXObject getXObject(@Nonnull final PagePreRenderContext aCtx) throws IOException, SVGException;

    @Override
    @Nonnull
    public EChange beforeRender(@Nonnull final PagePreRenderContext aCtx) {
        try {
            m_aXObject = getXObject(aCtx);
            if (m_aXObject == null)
                throw new IllegalStateException("Failed to create PDFormXObject");
        } catch (IOException | SVGException ex) {
            throw new IllegalArgumentException("Failed to create SVG", ex);
        }
        return EChange.UNCHANGED;
    }

    @Override
    protected void onRender(@Nonnull final PageRenderContext aCtx) throws IOException {
        // Fill and border
        PLRenderHelper.fillAndRenderBorder(thisAsT(), aCtx, 0f, 0f);

        final PDPageContentStreamWithCache aContentStream = aCtx.getContentStream();
        aContentStream.drawFormXObject(m_aXObject,
                aCtx.getStartLeft() + getOutlineLeft(),
                aCtx.getStartTop() - getOutlineTop() - m_fImageHeight,
                m_fImageWidth,
                m_fImageHeight);
    }

    @Override
    public String toString() {
        return ToStringGenerator.getDerived(super.toString())
                .append("ImageWidth", m_fImageWidth)
                .append("ImageHeight", m_fImageHeight)
                .append("ImageType", "svg")
                .getToString();
    }
}
