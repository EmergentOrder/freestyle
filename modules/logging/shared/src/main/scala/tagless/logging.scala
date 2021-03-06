/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

package freestyle.tagless

object logging {

  @tagless
  trait LoggingM {

    def debug(msg: String, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]

    def debugWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]

    def error(msg: String, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]

    def errorWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]

    def info(msg: String, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]

    def infoWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]

    def warn(msg: String, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]

    def warnWithCause(msg: String, cause: Throwable, sourceAndLineInfo: Boolean = false)(
        implicit line: sourcecode.Line,
        file: sourcecode.File): FS[Unit]
  }

}
