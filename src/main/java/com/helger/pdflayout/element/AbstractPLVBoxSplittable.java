/**
 * Copyright (C) 2014 Philip Helger (www.helger.com)
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
package com.helger.pdflayout.element;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.commons.typeconvert.TypeConverter;
import com.helger.pdflayout.PLDebug;
import com.helger.pdflayout.spec.SizeSpec;

/**
 * Vertical box - groups several rows.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type
 */
public abstract class AbstractPLVBoxSplittable <IMPLTYPE extends AbstractPLVBoxSplittable <IMPLTYPE>> extends AbstractPLVBox <IMPLTYPE> implements IPLSplittableElement
{
  public AbstractPLVBoxSplittable ()
  {}

  public boolean containsAnySplittableElement ()
  {
    for (final Row aRow : m_aRows)
      if (aRow.getElement ().isSplittable ())
        return true;
    return false;
  }

  @Nonnull
  @ReturnsMutableCopy
  private static float [] _getAsArray (@Nonnull final List <Float> aList)
  {
    return TypeConverter.convertIfNecessary (aList, float [].class);
  }

  @Nullable
  public PLSplitResult splitElements (final float fElementWidth, final float fAvailableHeight)
  {
    if (fAvailableHeight <= 0)
      return null;

    if (!containsAnySplittableElement ())
    {
      // Splitting makes no sense
      if (PLDebug.isDebugSplit ())
        PLDebug.debugSplit (this, "Cannot split because no splittable elements are contained");
      return null;
    }

    // Create resulting VBoxes - the first one is not splittable again!
    final PLVBox aVBox1 = new PLVBox ().setBasicDataFrom (this);
    final PLVBoxSplittable aVBox2 = new PLVBoxSplittable ().setBasicDataFrom (this);

    final int nTotalRows = getRowCount ();
    final List <Float> aVBox1RowWidthFull = new ArrayList <Float> (nTotalRows);
    final List <Float> aVBox1RowHeightFull = new ArrayList <Float> (nTotalRows);
    float fUsedVBox1Width = 0;
    float fUsedVBox1WidthFull = 0;
    float fUsedVBox1Height = 0;
    float fUsedVBox1HeightFull = 0;
    final List <Float> aVBox2RowWidthFull = new ArrayList <Float> (nTotalRows);
    final List <Float> aVBox2RowHeightFull = new ArrayList <Float> (nTotalRows);
    float fUsedVBox2Width = 0;
    float fUsedVBox2WidthFull = 0;
    float fUsedVBox2Height = 0;
    float fUsedVBox2HeightFull = 0;

    // Copy all content rows
    boolean bOnTable1 = true;

    for (int nRow = 0; nRow < nTotalRows; ++nRow)
    {
      final AbstractPLElement <?> aRowElement = getRowElementAtIndex (nRow);
      final float fRowWidth = m_aPreparedRowElementWidth[nRow];
      final float fRowWidthFull = fRowWidth + aRowElement.getMarginPlusPaddingXSum ();
      final float fRowHeight = m_aPreparedRowElementHeight[nRow];
      final float fRowHeightFull = fRowHeight + aRowElement.getMarginPlusPaddingYSum ();

      if (bOnTable1)
      {
        if (fUsedVBox1HeightFull + fRowHeightFull <= fAvailableHeight)
        {
          // Row fits in first VBox without a change
          aVBox1.addRow (aRowElement);
          fUsedVBox1Width = Math.max (fUsedVBox1Width, fRowWidth);
          fUsedVBox1WidthFull = Math.max (fUsedVBox1WidthFull, fRowWidthFull);
          fUsedVBox1Height += fRowHeight;
          fUsedVBox1HeightFull += fRowHeightFull;
          aVBox1RowWidthFull.add (Float.valueOf (fRowWidthFull));
          aVBox1RowHeightFull.add (Float.valueOf (fRowHeightFull));
        }
        else
        {
          // Row does not fit - check if it can be splitted
          bOnTable1 = false;
          // try to split the row
          boolean bSplittedRow = false;
          if (aRowElement.isSplittable ())
          {
            // don't override fVBox1Width
            final float fWidth = Math.max (fUsedVBox1Width, fRowWidth);
            final float fWidthFull = Math.max (fUsedVBox1WidthFull, fRowWidthFull);

            final float fAvailableSplitWidth = fWidth;
            final float fAvailableSplitHeight = fAvailableHeight -
                                                fUsedVBox1HeightFull -
                                                aRowElement.getMarginPlusPaddingYSum ();
            if (PLDebug.isDebugSplit ())
              PLDebug.debugSplit (this, "Trying to split " +
                                        aRowElement.getDebugID () +
                                        " into pieces for split width " +
                                        fAvailableSplitWidth +
                                        " and height " +
                                        fAvailableSplitHeight);

            // Try to split the element contained in the row
            final PLSplitResult aSplitResult = aRowElement.getAsSplittable ().splitElements (fAvailableSplitWidth,
                                                                                             fAvailableSplitHeight);

            if (aSplitResult != null)
            {
              final AbstractPLElement <?> aVBox1RowElement = aSplitResult.getFirstElement ().getElement ();
              aVBox1.addRow (aVBox1RowElement);
              fUsedVBox1Width = fWidth;
              fUsedVBox1WidthFull = fWidthFull;
              final float fVBox1RowHeight = aSplitResult.getFirstElement ().getHeight ();
              final float fVBox1RowHeightFull = fVBox1RowHeight + aVBox1RowElement.getMarginPlusPaddingYSum ();
              fUsedVBox1Height += fVBox1RowHeight;
              fUsedVBox1HeightFull += fVBox1RowHeightFull;
              aVBox1RowWidthFull.add (Float.valueOf (fWidthFull));
              aVBox1RowHeightFull.add (Float.valueOf (fVBox1RowHeightFull));

              final AbstractPLElement <?> aVBox2RowElement = aSplitResult.getSecondElement ().getElement ();
              aVBox2.addRow (aVBox2RowElement);
              fUsedVBox2Width = fWidth;
              fUsedVBox2WidthFull = fWidthFull;
              final float fVBox2RowHeight = aSplitResult.getSecondElement ().getHeight ();
              final float fVBox2RowHeightFull = fVBox2RowHeight + aVBox2RowElement.getMarginPlusPaddingYSum ();
              fUsedVBox2Height += fVBox2RowHeight;
              aVBox2RowWidthFull.add (Float.valueOf (fWidthFull));
              aVBox2RowHeightFull.add (Float.valueOf (fVBox2RowHeightFull));

              if (PLDebug.isDebugSplit ())
                PLDebug.debugSplit (this, "Split row element " +
                                          aRowElement.getDebugID () +
                                          " (Row " +
                                          nRow +
                                          ") into pieces: " +
                                          aVBox1RowElement.getDebugID () +
                                          " (" +
                                          aSplitResult.getFirstElement ().getWidth () +
                                          "+" +
                                          aVBox1RowElement.getMarginPlusPaddingXSum () +
                                          " & " +
                                          aSplitResult.getFirstElement ().getHeight () +
                                          "+" +
                                          aVBox1RowElement.getMarginPlusPaddingYSum () +
                                          ") and " +
                                          aVBox2RowElement.getDebugID () +
                                          " (" +
                                          aSplitResult.getSecondElement ().getWidth () +
                                          "+" +
                                          aVBox2RowElement.getMarginPlusPaddingXSum () +
                                          " & " +
                                          aSplitResult.getSecondElement ().getHeight () +
                                          "+" +
                                          aVBox2RowElement.getMarginPlusPaddingYSum () +
                                          ")");
              bSplittedRow = true;
            }
            else
            {
              if (PLDebug.isDebugSplit ())
                PLDebug.debugSplit (this, "Failed to split row element " +
                                          aRowElement.getDebugID () +
                                          " (Row " +
                                          nRow +
                                          ") into pieces");
            }
          }

          if (!bSplittedRow)
          {
            // just add the full row to the second VBox since the row does not
            // fit on first page
            aVBox2.addRow (aRowElement);
            fUsedVBox2Width = Math.max (fUsedVBox2Width, fRowWidth);
            fUsedVBox2WidthFull = Math.max (fUsedVBox2WidthFull, fRowWidthFull);
            fUsedVBox2Height += fRowHeight;
            fUsedVBox2HeightFull += fRowHeightFull;
            aVBox2RowWidthFull.add (Float.valueOf (fRowWidthFull));
            aVBox2RowHeightFull.add (Float.valueOf (fRowHeightFull));
          }
        }
      }
      else
      {
        // We're already on VBox 2 - add all elements, since VBox2 may be split
        // again!
        aVBox2.addRow (aRowElement);
        fUsedVBox2Width = Math.max (fUsedVBox2Width, fRowWidth);
        fUsedVBox2WidthFull = Math.max (fUsedVBox2WidthFull, fRowWidthFull);
        fUsedVBox2Height += fRowHeight;
        fUsedVBox2HeightFull += fRowHeightFull;
        aVBox2RowWidthFull.add (Float.valueOf (fRowWidthFull));
        aVBox2RowHeightFull.add (Float.valueOf (fRowHeightFull));
      }
    }

    if (aVBox1.getRowCount () == 0)
    {
      // Splitting makes no sense!
      if (PLDebug.isDebugSplit ())
        PLDebug.debugSplit (this, "Splitting makes no sense, because VBox 1 would be empty");
      return null;
    }

    if (aVBox2.getRowCount () == 0)
    {
      // Splitting makes no sense!
      if (PLDebug.isDebugSplit ())
        PLDebug.debugSplit (this, "Splitting makes no sense, because VBox 2 would be empty");
      return null;
    }

    // Excluding padding/margin
    aVBox1.markAsPrepared (new SizeSpec (fElementWidth, fUsedVBox1HeightFull));
    aVBox1.m_aPreparedRowElementWidth = _getAsArray (aVBox1RowWidthFull);
    aVBox1.m_aPreparedRowElementHeight = _getAsArray (aVBox1RowHeightFull);

    aVBox2.markAsPrepared (new SizeSpec (fElementWidth, fUsedVBox2HeightFull));
    aVBox2.m_aPreparedRowElementWidth = _getAsArray (aVBox2RowWidthFull);
    aVBox2.m_aPreparedRowElementHeight = _getAsArray (aVBox2RowHeightFull);

    return new PLSplitResult (new PLElementWithSize (aVBox1, new SizeSpec (fElementWidth, fUsedVBox1HeightFull)),
                              new PLElementWithSize (aVBox2, new SizeSpec (fElementWidth, fUsedVBox2HeightFull)));
  }
}
