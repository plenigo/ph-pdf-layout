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

import javax.annotation.Nonnull;

/**
 * Horizontal box - groups several columns without having layout information
 * itself.
 *
 * @author plenigo
 */
public class PLMultiLineTextBox extends AbstractPLMultiLineTextBox<PLMultiLineTextBox>
{
  /**
   * Default constructor for an empty HBox.
   */
  public PLMultiLineTextBox()
  {}

  @Override
  @Nonnull
  public PLMultiLineTextBox internalCreateNewVertSplitObject (@Nonnull final PLMultiLineTextBox aBase)
  {
    final PLMultiLineTextBox ret = new PLMultiLineTextBox();
    ret.setBasicDataFrom (aBase);
    return ret;
  }
}
