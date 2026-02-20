package com.plenigo.pdflayout.element.svg;

import com.helger.base.state.EChange;
import com.helger.base.tostring.ToStringGenerator;
import org.jspecify.annotations.NonNull;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.base.enforce.ValueEnforcer;

import com.kitfox.svg.SVGException;
import com.plenigo.pdflayout.base.AbstractPLInlineElement;
import com.plenigo.pdflayout.pdfbox.PDPageContentStreamWithCache;
import com.plenigo.pdflayout.render.PLRenderHelper;
import com.plenigo.pdflayout.render.PagePreRenderContext;
import com.plenigo.pdflayout.render.PageRenderContext;
import com.plenigo.pdflayout.render.PreparationContext;
import com.plenigo.pdflayout.spec.SizeSpec;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;


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
    @NonNull
    @OverridingMethodsMustInvokeSuper
    public IMPLTYPE setBasicDataFrom(@NonNull final IMPLTYPE aSource) {
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
    protected SizeSpec onPrepare(@NonNull final PreparationContext aCtx) {
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
    @NonNull
    protected abstract PDFormXObject getXObject(@NonNull final PagePreRenderContext aCtx) throws IOException, SVGException;

    @Override
    @NonNull
    public EChange beforeRender(@NonNull final PagePreRenderContext aCtx) {
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
    protected void onRender(@NonNull final PageRenderContext aCtx) throws IOException {
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
