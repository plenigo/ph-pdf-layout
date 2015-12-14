package com.helger.pdflayout.util;

import java.util.Arrays;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;

/**
 * Special int-float-primitive map. Based on:
 * https://github.com/mikvor/hashmapTest
 *
 * @author Mikhail Vorontsov
 * @author Philip Helger
 */
@NotThreadSafe
public class IntFloatMap
{
  private static final int FREE_KEY = 0;

  public static final float NO_VALUE = Float.NEGATIVE_INFINITY;

  /** Keys */
  private int [] m_aKeys;
  /** Values */
  private float [] m_aValues;

  /** Do we have 'free' key in the map? */
  private boolean m_bHasFreeKey;
  /** Value of 'free' key */
  private float m_aFreeValue = NO_VALUE;

  /** Fill factor, must be between (0 and 1) */
  private final float m_fFillFactor;
  /** We will resize a map once it reaches this size */
  private int m_nThreshold;
  /** Current map size */
  private int m_nSize;
  /** Mask to calculate the original position */
  private int m_nMask;

  public IntFloatMap ()
  {
    this (16);
  }

  public IntFloatMap (final int nSize)
  {
    this (nSize, 0.75f);
  }

  public IntFloatMap (final int nSize, final float fFillFactor)
  {
    ValueEnforcer.isBetweenInclusive (fFillFactor, "FillFactor", 0f, 1f);
    ValueEnforcer.isGT0 (nSize, "Size");
    final int nCapacity = MapTools.arraySize (nSize, fFillFactor);
    m_nMask = nCapacity - 1;
    m_fFillFactor = fFillFactor;

    m_aKeys = new int [nCapacity];
    m_aValues = _createValueArray (nCapacity);
    m_nThreshold = (int) (nCapacity * fFillFactor);
  }

  @Nonnull
  @ReturnsMutableCopy
  private static float [] _createValueArray (@Nonnegative final int nSize)
  {
    final float [] ret = new float [nSize];
    Arrays.fill (ret, NO_VALUE);
    return ret;
  }

  public float get (final int key)
  {
    return get (key, NO_VALUE);
  }

  public float get (final int key, final float fDefault)
  {
    if (key == FREE_KEY)
      return m_bHasFreeKey ? m_aFreeValue : fDefault;

    final int idx = _getReadIndex (key);
    return idx != -1 ? m_aValues[idx] : fDefault;
  }

  public float put (final int key, final float value)
  {
    if (key == FREE_KEY)
    {
      final float ret = m_aFreeValue;
      if (!m_bHasFreeKey)
      {
        ++m_nSize;
        m_bHasFreeKey = true;
      }
      m_aFreeValue = value;
      return ret;
    }

    int idx = _getPutIndex (key);
    if (idx < 0)
    {
      // no insertion point? Should not happen...
      _rehash (m_aKeys.length * 2);
      idx = _getPutIndex (key);
    }
    final float prev = m_aValues[idx];
    if (m_aKeys[idx] != key)
    {
      m_aKeys[idx] = key;
      m_aValues[idx] = value;
      ++m_nSize;
      if (m_nSize >= m_nThreshold)
        _rehash (m_aKeys.length * 2);
    }
    else
    {
      // it means used cell with our key
      assert m_aKeys[idx] == key;
      m_aValues[idx] = value;
    }
    return prev;
  }

  public float remove (final int key)
  {
    if (key == FREE_KEY)
    {
      if (!m_bHasFreeKey)
        return NO_VALUE;
      m_bHasFreeKey = false;
      final float ret = m_aFreeValue;
      m_aFreeValue = NO_VALUE;
      --m_nSize;
      return ret;
    }

    final int idx = _getReadIndex (key);
    if (idx == -1)
      return NO_VALUE;

    final float res = m_aValues[idx];
    m_aValues[idx] = NO_VALUE;
    _shiftKeys (idx);
    --m_nSize;
    return res;
  }

  @Nonnegative
  public int size ()
  {
    return m_nSize;
  }

  private void _rehash (final int nNewCapacity)
  {
    m_nThreshold = (int) (nNewCapacity * m_fFillFactor);
    m_nMask = nNewCapacity - 1;

    final int oldCapacity = m_aKeys.length;
    final int [] oldKeys = m_aKeys;
    final float [] oldValues = m_aValues;

    m_aKeys = new int [nNewCapacity];
    m_aValues = _createValueArray (nNewCapacity);
    m_nSize = m_bHasFreeKey ? 1 : 0;

    for (int i = oldCapacity; i-- > 0;)
      if (oldKeys[i] != FREE_KEY)
        put (oldKeys[i], oldValues[i]);
  }

  private int _shiftKeys (final int nPos)
  {
    // Shift entries with the same hash.
    int last, slot, pos = nPos;
    int k;
    final int [] keys = m_aKeys;
    while (true)
    {
      last = pos;
      pos = _getNextIndex (pos);
      while (true)
      {
        k = keys[pos];
        if (k == FREE_KEY)
        {
          keys[last] = FREE_KEY;
          m_aValues[last] = NO_VALUE;
          return last;
        }
        // calculate the starting slot for the current key
        slot = MapTools.phiMix (k) & m_nMask;
        if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos)
          break;
        pos = _getNextIndex (pos);
      }
      keys[last] = k;
      m_aValues[last] = m_aValues[pos];
    }
  }

  /**
   * Find key position in the map.
   *
   * @param key
   *        Key to look for
   * @return Key position or -1 if not found
   */
  @CheckForSigned
  private int _getReadIndex (final int key)
  {
    int idx = MapTools.phiMix (key) & m_nMask;
    if (m_aKeys[idx] == key)
    {
      // we check FREE prior to this call
      return idx;
    }
    if (m_aKeys[idx] == FREE_KEY)
    {
      // end of chain already
      return -1;
    }
    final int startIdx = idx;
    while ((idx = _getNextIndex (idx)) != startIdx)
    {
      if (m_aKeys[idx] == FREE_KEY)
        return -1;
      if (m_aKeys[idx] == key)
        return idx;
    }
    return -1;
  }

  /**
   * Find an index of a cell which should be updated by 'put' operation. It can
   * be: 1) a cell with a given key 2) first free cell in the chain
   *
   * @param key
   *        Key to look for
   * @return Index of a cell to be updated by a 'put' operation
   */
  @CheckForSigned
  private int _getPutIndex (final int key)
  {
    final int readIdx = _getReadIndex (key);
    if (readIdx >= 0)
      return readIdx;
    // key not found, find insertion point
    final int startIdx = MapTools.phiMix (key) & m_nMask;
    if (m_aKeys[startIdx] == FREE_KEY)
      return startIdx;
    int idx = startIdx;
    while (m_aKeys[idx] != FREE_KEY)
    {
      idx = _getNextIndex (idx);
      if (idx == startIdx)
        return -1;
    }
    return idx;
  }

  private int _getNextIndex (final int currentIndex)
  {
    return (currentIndex + 1) & m_nMask;
  }
}