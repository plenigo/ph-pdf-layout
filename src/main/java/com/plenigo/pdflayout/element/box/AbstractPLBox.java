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
package com.plenigo.pdflayout.element.box;

import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.plenigo.pdflayout.base.AbstractPLBlockElement;
import com.plenigo.pdflayout.base.AbstractPLRenderableObject;
import com.plenigo.pdflayout.base.IPLRenderableObject;
import com.plenigo.pdflayout.base.IPLSplittableObject;
import com.plenigo.pdflayout.base.IPLVisitor;
import com.plenigo.pdflayout.base.PLElementWithSize;
import com.plenigo.pdflayout.base.PLSplitResult;
import com.plenigo.pdflayout.debug.PLDebugLog;
import com.plenigo.pdflayout.render.PLRenderHelper;
import com.plenigo.pdflayout.render.PageRenderContext;
import com.plenigo.pdflayout.render.PreparationContext;
import com.plenigo.pdflayout.spec.SizeSpec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;

/**
 * A box is a simple element that encapsulates another element and has a
 * padding, border and margin itself as well as it can align the contained
 * element.
 *
 * @param <IMPLTYPE> Implementation type
 *
 * @author Philip Helger
 */
public abstract class AbstractPLBox<IMPLTYPE extends AbstractPLBox<IMPLTYPE>> extends AbstractPLBlockElement<IMPLTYPE> implements
        IPLSplittableObject<IMPLTYPE, IMPLTYPE> {
    private IPLRenderableObject<?> m_aElement;
    private boolean m_bVertSplittable = DEFAULT_VERT_SPLITTABLE;

    // Status vars
    private SizeSpec m_aElementPreparedSize;
    private SizeSpec m_aRenderOffset = SizeSpec.SIZE0;

    public AbstractPLBox(@Nullable final IPLRenderableObject<?> aElement) {
        setElement(aElement);
    }

  @Override
  @Nonnull
  @OverridingMethodsMustInvokeSuper
  public IMPLTYPE setBasicDataFrom (@Nonnull final IMPLTYPE aSource)
  {
    super.setBasicDataFrom (aSource);
    setVertSplittable (aSource.isVertSplittable ());
    return thisAsT ();
  }

  /**
   * @return The element passed in the constructor. May be <code>null</code>.
   */
  @Nullable
  public final IPLRenderableObject <?> getElement ()
  {
    return m_aElement;
  }

  /**
   * @return <code>true</code> if an element is contained, <code>false</code> if
   *         not.
   */
  public final boolean hasElement ()
  {
    return m_aElement != null;
  }

  @Nonnull
  public final IMPLTYPE setElement (@Nullable final IPLRenderableObject <?> aElement)
  {
    internalCheckNotPrepared ();
    m_aElement = aElement;
    return thisAsT ();
  }

  public final boolean isVertSplittable ()
  {
    if (!m_bVertSplittable)
      return false;
    // Empty boxes or boxes with a non-splittable element cannot be split
    return hasElement () && getElement ().isVertSplittable ();
  }

  @Nonnull
  public final IMPLTYPE setVertSplittable (final boolean bVertSplittable)
  {
    m_bVertSplittable = bVertSplittable;
    return thisAsT ();
  }

  @Override
  @Nonnull
  public EChange visit (@Nonnull final IPLVisitor aVisitor) throws IOException
  {
    EChange ret = super.visit (aVisitor);
    if (m_aElement != null)
    {
      if (m_aElement.visit (aVisitor).isChanged ())
      {
        ret = EChange.CHANGED;

        // Something changed in the contained element
        // E.g. in onBeforeRender for text elements with placeholder texts
        // replaced
        final SizeSpec aElementPreparedSize = m_aElement.getPreparedSize ();
        internalMarkAsNotPreparedDontPropagate ();
        internalSetElementPreparedSize (aElementPreparedSize);
        internalMarkAsPrepared (aElementPreparedSize.plus (m_aElement.getOutlineXSum (), m_aElement.getOutlineYSum ()));
      }
    }
    return ret;
  }

  /**
   * @return The prepared size of the contained element. May be
   *         <code>null</code> if this box was not yet prepared or if no element
   *         is contained.
   */
  @Nullable
  protected final SizeSpec getElementPreparedSize ()
  {
    return m_aElementPreparedSize;
  }

  protected final void internalSetElementPreparedSize (@Nullable final SizeSpec aSize)
  {
    m_aElementPreparedSize = aSize;
  }

  @Override
  @Nonnull
  protected SizeSpec getRenderSize (@Nonnull final SizeSpec aPreparedSize)
  {
    SizeSpec aRenderSize = super.getRenderSize (aPreparedSize);

    if (isFullWidth ())
    {
      // Change render size before render offset, so that internal alignment
      // works
      aRenderSize = aRenderSize.withWidth (getPrepareAvailableSize ().getWidth () - getOutlineXSum ());
    }

    // Handle horizontal and vertical alignment here
    m_aRenderOffset = new SizeSpec (getIndentX (aRenderSize.getWidth (), aPreparedSize.getWidth ()),
                                    getIndentY (aRenderSize.getHeight (), aPreparedSize.getHeight ()));

    return aRenderSize;
  }

  @Override
  protected SizeSpec onPrepare (@Nonnull final PreparationContext aCtx)
  {
    if (m_aElement == null)
    {
      // No content - no size
      return SizeSpec.SIZE0;
    }

    final float fElementWidth = aCtx.getAvailableWidth () - getOutlineXSum ();
    final float fElementHeight = aCtx.getAvailableHeight () - getOutlineYSum ();

    final PreparationContext aElementCtx = new PreparationContext (aCtx.getGlobalContext (), fElementWidth, fElementHeight);
    internalSetElementPreparedSize (m_aElement.prepare (aElementCtx));

    // Add the outer stuff of the contained element as this elements prepared
    // size
    return new SizeSpec (m_aElementPreparedSize.getWidth () + m_aElement.getOutlineXSum (),
                         m_aElementPreparedSize.getHeight () + m_aElement.getOutlineYSum ());
  }

  @Override
  protected void onMarkAsNotPrepared () {
      internalSetElementPreparedSize(null);
      if (m_aElement instanceof AbstractPLRenderableObject<?>)
          ((AbstractPLRenderableObject<?>) m_aElement).internalMarkAsNotPrepared();
  }

  @Nullable
  public PLSplitResult splitElementVert (final float fAvailableWidth, final float fAvailableHeight)
  {
    if (fAvailableHeight <= 0)
      return null;

    final float fBoxHeight = getPreparedHeight ();
    if (fBoxHeight <= fAvailableHeight)
    {
      // Splitting makes no sense!
      if (PLDebugLog.isDebugSplit ())
        PLDebugLog.debugSplit (this, "Splitting makes no sense, because Box 2 would be empty");
      return null;
    }

    final IPLRenderableObject <?> aElement = getElement ();

    // Create resulting VBoxes - the first one is not splittable again!
    final AbstractPLBox <?> aBox1 = internalCreateNewVertSplitObject (thisAsT ()).setID (getID () + "-1").setVertSplittable (false);
    final AbstractPLBox <?> aBox2 = internalCreateNewVertSplitObject (thisAsT ()).setID (getID () + "-2").setVertSplittable (true);

    // Set min width/max width from source
    // Don't use the height, because on vertically split elements, the height is
    // dynamic
    aBox1.setMinWidth (getMinWidth ());
    aBox1.setMaxWidth (getMaxWidth ());
    aBox2.setMinWidth (getMinWidth ());
    aBox2.setMaxWidth (getMaxWidth ());

    float fBox1UsedHeight = 0;
    float fBox2UsedHeight = 0;

    SizeSpec aBox1ElementPreparedSize = null;
    SizeSpec aBox2ElementPreparedSize = null;

    // Try split
    final float fSplitWidth = getElementPreparedSize ().getWidth ();
    final float fSplitHeight = fAvailableHeight - aElement.getOutlineYSum ();
    if (PLDebugLog.isDebugSplit ())
      PLDebugLog.debugSplit (this,
                             "Trying to split " +
                                   aElement.getDebugID () +
                                   " into pieces for split size " +
                                   PLDebugLog.getWH (fSplitWidth, fSplitHeight));

    // Try to split the element contained in the row
    final PLSplitResult aSplitResult = aElement.getAsSplittable ().splitElementVert (fSplitWidth, fSplitHeight);
    if (aSplitResult == null)
    {
      // Splitting makes no sense!
      if (PLDebugLog.isDebugSplit ())
        PLDebugLog.debugSplit (this, "Splitting makes no sense, because Box 2 would be empty");
      return null;
    }

    // Splitting succeeded
    final IPLRenderableObject <?> aBox1Element = aSplitResult.getFirstElement ().getElement ();
    aBox1.setElement (aBox1Element);
    fBox1UsedHeight += aSplitResult.getFirstElement ().getHeightFull ();
    aBox1ElementPreparedSize = aSplitResult.getFirstElement ().getSize ();

    final IPLRenderableObject <?> aBox2Element = aSplitResult.getSecondElement ().getElement ();
    aBox2.setElement (aBox2Element);
    fBox2UsedHeight += aSplitResult.getSecondElement ().getHeightFull ();
    aBox2ElementPreparedSize = aSplitResult.getSecondElement ().getSize ();

    if (PLDebugLog.isDebugSplit ())
      PLDebugLog.debugSplit (this,
                             "Split box element " +
                                   aElement.getDebugID () +
                                   " into pieces: " +
                                   aBox1Element.getDebugID () +
                                   " (" +
                                   aSplitResult.getFirstElement ().getWidth () +
                                   "+" +
                                   aBox1Element.getOutlineXSum () +
                                   " & " +
                                   aSplitResult.getFirstElement ().getHeight () +
                                   "+" +
                                   aBox1Element.getOutlineYSum () +
                                   ") and " +
                                   aBox2Element.getDebugID () +
                                   " (" +
                                   aSplitResult.getSecondElement ().getWidth () +
                                   "+" +
                                   aBox2Element.getOutlineXSum () +
                                   " & " +
                                   aSplitResult.getSecondElement ().getHeight () +
                                   "+" +
                                     aBox2Element.getOutlineYSum() +
                                     ")");

      // Excluding padding/margin
      aBox1.internalMarkAsPrepared(new SizeSpec(fAvailableWidth, fBox1UsedHeight));
      aBox1.internalSetElementPreparedSize(aBox1ElementPreparedSize);

      aBox2.internalMarkAsPrepared(new SizeSpec(fAvailableWidth, fBox2UsedHeight));
      aBox2.internalSetElementPreparedSize(aBox2ElementPreparedSize);

      return new PLSplitResult(new PLElementWithSize(aBox1, new SizeSpec(fAvailableWidth, fBox1UsedHeight)),
              new PLElementWithSize(aBox2, new SizeSpec(fAvailableWidth, fBox2UsedHeight)));
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  protected void onRender (@Nonnull final PageRenderContext aCtx) throws IOException
  {
    // Fill and border
    PLRenderHelper.fillAndRenderBorder (thisAsT (), aCtx, 0f, 0f);

    if (m_aElement != null)
    {
      final float fStartLeft = aCtx.getStartLeft () + getOutlineLeft () + m_aRenderOffset.getWidth ();
      final float fStartTop = aCtx.getStartTop () - getOutlineTop () - m_aRenderOffset.getHeight ();
      final PageRenderContext aElementCtx = new PageRenderContext (aCtx, fStartLeft, fStartTop, getRenderWidth (), getRenderHeight ());
      m_aElement.render (aElementCtx);
    }
    else
      PLDebugLog.debugRender (this, "Not rendering the box, because no element is contained");
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .appendIfNotNull ("Element", m_aElement)
                            .append ("VertSplittable", m_bVertSplittable)
                            .appendIfNotNull ("ElementPreparedSize", m_aElementPreparedSize)
                            .appendIfNotNull ("RenderOffset", m_aRenderOffset)
                            .getToString ();
  }
}
