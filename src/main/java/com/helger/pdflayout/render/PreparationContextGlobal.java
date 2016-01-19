/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.pdflayout.render;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.helger.commons.ValueEnforcer;
import com.helger.pdflayout.spec.FontSpec;
import com.helger.pdflayout.spec.LoadedFont;
import com.helger.pdflayout.spec.PreloadFont;

/**
 * The current global context for preparing an element. This object must be the
 * same for all prepared elements.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class PreparationContextGlobal
{
  private final PDDocument m_aDoc;
  private final Map <PreloadFont, LoadedFont> m_aFontCache = new HashMap <PreloadFont, LoadedFont> ();

  /**
   * Constructor
   *
   * @param aDoc
   *        The {@link PDDocument} worked upon
   */
  public PreparationContextGlobal (@Nonnull final PDDocument aDoc)
  {
    ValueEnforcer.notNull (aDoc, "PDDocument");
    m_aDoc = aDoc;
  }

  @Nonnull
  public PDDocument getDocument ()
  {
    return m_aDoc;
  }

  @Nonnull
  public LoadedFont getLoadedFont (@Nonnull final FontSpec aFontSpec) throws IOException
  {
    final PreloadFont aPreloadFont = aFontSpec.getPreloadFont ();
    LoadedFont aLoadedFont = m_aFontCache.get (aPreloadFont);
    if (aLoadedFont == null)
    {
      aLoadedFont = new LoadedFont (aPreloadFont.loadPDFont (m_aDoc));
      m_aFontCache.put (aPreloadFont, aLoadedFont);
    }
    return aLoadedFont;
  }
}
