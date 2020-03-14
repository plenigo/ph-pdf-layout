package com.helger.pdflayout4.element.list;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import com.helger.pdflayout4.base.IPLRenderableObject;
import com.helger.pdflayout4.spec.FontSpec;
import com.helger.pdflayout4.spec.PreloadFont;

/**
 * An implementation of {@link IBulletPointCreator} that always uses the Bullet
 * point character from symbol font.
 *
 * @author Philip Helger
 * @since 5.0.10
 */
public class BulletPointCreatorSymbol extends BulletPointCreatorConstant
{
  public BulletPointCreatorSymbol (@Nonnegative final float fFontSize)
  {
    // 183 or 176
    super (false ? " \u00b7" : " \u00b0", new FontSpec (PreloadFont.SYMBOL, fFontSize));
  }

  @Override
  @Nonnull
  public IPLRenderableObject <?> getBulletPointElement (@Nonnegative final int nBulletPointIndex)
  {
    final IPLRenderableObject <?> ret = super.getBulletPointElement (nBulletPointIndex);
    return ret;
  }
}