package com.plenigo.pdflayout.element.svg;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.string.ToStringGenerator;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.plenigo.pdflayout.render.PagePreRenderContext;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * Represent a static svg based on svg byte array.
 */
public class PLSvg extends AbstractPLSvg<PLSvg> {
    private final byte[] m_aSvg;

    public PLSvg(@Nonnull final byte[] aSvg, @Nonnegative final float fImageWidth, @Nonnegative final float fImageHeight) {
        super(fImageWidth, fImageHeight);
        ValueEnforcer.notNull(aSvg, "Svg");
        m_aSvg = aSvg;
    }

    @Override
    @Nonnull
    @OverridingMethodsMustInvokeSuper
    public PLSvg setBasicDataFrom(@Nonnull final PLSvg aSource) {
        super.setBasicDataFrom(aSource);
        return this;
    }

    @Nullable
    public byte[] getSvg() {
        return m_aSvg;
    }

    @Override
    @Nonnull
    protected PDFormXObject getXObject(@Nonnull final PagePreRenderContext aCtx) throws IOException, SVGException {
        SVGUniverse svgUniverse = new SVGUniverse();
        PdfBoxGraphics2D graphics;

        try (final NonBlockingByteArrayInputStream svgInputStream = new NonBlockingByteArrayInputStream(getSvg())) {
            SVGDiagram diagram = svgUniverse.getDiagram(svgUniverse.loadSVG(svgInputStream, "svgImage"));
            graphics = new PdfBoxGraphics2D(aCtx.getDocument(), diagram.getRoot().getDeviceWidth(), diagram.getRoot().getDeviceHeight());
            try {
                diagram.render(graphics);
            } finally {
                graphics.dispose();
            }
        }

        return graphics.getXFormObject();
    }

    @Override
    public String toString() {
        return ToStringGenerator.getDerived(super.toString()).append("Svg", m_aSvg).getToString();
    }
}
