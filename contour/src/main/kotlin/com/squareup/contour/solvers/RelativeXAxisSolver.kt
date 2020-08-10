/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.contour.solvers

import android.view.View
import com.squareup.contour.ContourLayout.LayoutSpec
import com.squareup.contour.HasEnd
import com.squareup.contour.HasStart
import com.squareup.contour.LayoutContainer
import com.squareup.contour.SizeMode
import com.squareup.contour.WidthOfOnlyContext
import com.squareup.contour.XFloat
import com.squareup.contour.XInt
import com.squareup.contour.constraints.Constraint
import com.squareup.contour.constraints.RelativePositionConstraint
import com.squareup.contour.utils.unwrapXFloatLambda
import com.squareup.contour.utils.unwrapXIntLambda
import java.lang.AssertionError
import kotlin.math.abs

internal class RelativeXAxisSolver(
  point: Point,
  lambda: LayoutContainer.() -> Int
) : XAxisSolver, HasStart, HasEnd, WidthOfOnlyContext {

  internal enum class Point {
    Start,
    End
  }

  private lateinit var parent: LayoutSpec

  private val p0 = RelativePositionConstraint(point, lambda)
  private val p1 = RelativePositionConstraint()
  private val size = Constraint()

  private var lastRtl: Boolean? = null
    set(value) {
      if (field != value) {
        field = value
        clear()
      }
    }

  private var start = Int.MIN_VALUE
  private var mid = Int.MIN_VALUE
  private var end = Int.MIN_VALUE

  private var range = Int.MIN_VALUE

  private fun start(): Int {
    if (start == Int.MIN_VALUE) {
      if (p0.point == Point.Start) {
        start = p0.resolve()
      } else {
        parent.measureSelf()
        resolveAxis()
      }
    }
    return start
  }

  private fun end(): Int {
    if (end == Int.MIN_VALUE) {
      if (p0.point == Point.End) {
        end = p0.resolve()
      } else {
        parent.measureSelf()
        resolveAxis()
      }
    }
    return end
  }

  override fun min(rtl: Boolean): Int {
    lastRtl = rtl

    return if (rtl) {
      end()
    } else {
      start()
    }
  }

  override fun mid(rtl: Boolean): Int {
    lastRtl = rtl

    if (mid == Int.MIN_VALUE) {
      parent.measureSelf()
      resolveAxis()
    }
    return mid
  }

  override fun baseline(): Int {
    throw AssertionError()
  }

  override fun max(rtl: Boolean): Int {
    lastRtl = rtl

    return if (rtl) {
      start()
    } else {
      end()
    }
  }

  override fun range(): Int {
    if (range == Int.MIN_VALUE) {
      parent.measureSelf()
    }
    return range
  }

  private fun resolveAxis() {
    check(range != Int.MIN_VALUE)

    val hV = range / 2
    when (p0.point) {
      Point.Start -> {
        start = p0.resolve()
        mid = start + hV
        end = start + range
      }
      Point.End -> {
        end = p0.resolve()
        mid = end - hV
        start = end - range
      }
    }
  }

  override fun onAttach(parent: LayoutSpec) {
    this.parent = parent
    p0.onAttachContext(parent)
    p1.onAttachContext(parent)
    size.onAttachContext(parent)
  }

  override fun onRangeResolved(range: Int, baselineRange: Int) {
    this.range = range
  }

  override fun measureSpec(): Int {
    return if (p1.isSet) {
      View.MeasureSpec.makeMeasureSpec(abs(p0.resolve() - p1.resolve()), p1.mode.mask)
    } else if (size.isSet) {
      View.MeasureSpec.makeMeasureSpec(size.resolve(), size.mode.mask)
    } else {
      0
    }
  }

  override fun clear() {
    start = Int.MIN_VALUE
    mid = Int.MIN_VALUE
    end = Int.MIN_VALUE
    range = Int.MIN_VALUE
    p0.clear()
    p1.clear()
    size.clear()
  }

  override fun startTo(
    mode: SizeMode,
    provider: LayoutContainer.() -> XInt
  ): XAxisSolver {
    p1.point = Point.Start
    p1.mode = mode
    p1.lambda = unwrapXIntLambda(provider)
    return this
  }

  override fun startToFloat(
    mode: SizeMode,
    provider: LayoutContainer.() -> XFloat
  ): XAxisSolver {
    p1.point = Point.Start
    p1.mode = mode
    p1.lambda = unwrapXFloatLambda(provider)
    return this
  }

  override fun endTo(
    mode: SizeMode,
    provider: LayoutContainer.() -> XInt
  ): XAxisSolver {
    p1.point = Point.End
    p1.mode = mode
    p1.lambda = unwrapXIntLambda(provider)
    return this
  }

  override fun endToFloat(
    mode: SizeMode,
    provider: LayoutContainer.() -> XFloat
  ): XAxisSolver {
    p1.point = Point.End
    p1.mode = mode
    p1.lambda = unwrapXFloatLambda(provider)
    return this
  }

  override fun widthOf(mode: SizeMode, provider: LayoutContainer.() -> XInt): XAxisSolver {
    size.mode = mode
    size.lambda = unwrapXIntLambda(provider)
    return this
  }

  override fun widthOfFloat(
    mode: SizeMode,
    provider: LayoutContainer.() -> XFloat
  ): XAxisSolver {
    size.mode = mode
    size.lambda = unwrapXFloatLambda(provider)
    return this
  }
}
