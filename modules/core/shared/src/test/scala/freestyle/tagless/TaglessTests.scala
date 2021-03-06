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

import org.scalatest.{Matchers, WordSpec}

import freestyle.free._

import cats.{~>, Id, Functor, Applicative, Monad, Monoid, Eq}
import cats.arrow.FunctionK
import cats.free.Free
import cats.instances.either._
import cats.instances.option._
import cats.kernel.instances.int._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._

import algebras._
import handlers._
import modules._

class TaglessTests extends WordSpec with Matchers {

  "the @tagless macro annotation should be accepted if it is applied to" when {

    "a trait with at least one request" in {
      "@tagless trait X { def bar(x:Int): FS[Int] }" should compile
    }

    "a trait with a kind-1 type param" when {
      "typing the request with reserved FS" in {
        "@tagless trait FBound[F[_]] { def ann(x:Int): FS[Int] }" should compile
      }
      "typing the request with the user-provided F-Bound type param" in {
        "@tagless trait FBound[F[_]] { def bob(y:Int): F[Int] }" should compile
      }
    }

    "an abstract class with at least one request" in {
      "@tagless abstract class X { def bar(x:Int): FS[Int] }" should compile
    }

    "a trait with an abstact method of type FS" in {
      "@tagless trait X { def f(a: Char) : FS[Int] }" should compile
    }

    "a trait with type parameters" ignore {
      "@tagless trait X[A] { def ix(a: A) : FS[A] }" should compile
    }

    "a trait with some concrete non-FS members" ignore {
      """@tagless trait X {
        def x: FS[Int]
        def y: Int = 5
        val z: Int = 6
      }""" should compile
    }

    "a trait with a method with type parameters" in {
      "@tagless trait WiX { def ix[A](a: A) : FS[A] }" should compile
    }

    "a trait with high bounded type parameters in the method" in {
      "@tagless trait X { def ix[A <: Int](a: A) : FS[A] }" should compile
    }

    "a trait with lower bounded type parameters in the method" in {
      "@tagless trait X { def ix[A >: Int](a: A) : FS[A] }" should compile
    }

    "a trait with different type parameters in the method" in {
      "@tagless trait X { def ix[A <: Int, B, C >: Int](a: A, b: B, c: C) : FS[A] }" should compile
    }

    "a trait with high bounded type parameters and implicits in the method" in {
      """
        trait X[A]
        @tagless trait Y { def ix[A <: Int : X](a: A) : FS[A] }
      """ should compile
    }

  }

  "the @tagless macro should preserve the shape of the parameters of the request" when {

    "there are no parameters" in {
      @tagless trait X { def f: FS[Int] }
      object Y extends X.Handler[Id] { def f: Int = 42 }

      Y.f shouldEqual 42
    }

    "there is one list with multiple params" in {
      @tagless trait X {
        def f(a: Int, b: Int): FS[Int]
      }
      object Y extends X.Handler[Id] {
        override def f(a: Int, b: Int): Int = 42
      }
      Y.f(2,3) shouldEqual 42
    }

    "there are multiple lists of parameters" ignore {
      @tagless trait X {
        def f(a: Int)(b: Int): FS[Int]
      }
      object Y extends X.Handler[Id] {
        override def f(a: Int)(b: Int): Int = 42
      }
      Y.f(2)(3) shouldEqual 42
    }

    "there are multiple lists of parameters, with the last being implicit" ignore {
      @tagless trait X {
        def f(a: Int)(implicit b: Int): FS[Int]
      }
      object Y extends X.Handler[Id] {
        override def f(a: Int)(implicit b: Int): Int = 42
      }
      implicit val x: Int = 3
      Y.f(4) shouldEqual 42
    }

    "there is one type parameter with a type-class bound, and one parameter" ignore {
      @tagless trait X {
        def f[T: Monoid](a: T): FS[T]
        def g[S: Eq](a: S, b: S): FS[Boolean]
      }
      object Y extends X.Handler[Id]{
        def f[T: Monoid](a: T): T = a
        def g[S: Eq](a: S, b: S): Boolean = Eq[S].eqv(a,b)
      }
      Y.f[Int](42)
      Y.g[Int](42, 42)
    }

  }

  "the @tagless macro annotation should be rejected, and the compilation fail, if it is applied to" when {

    "an empty trait" in (
      "@tagless trait X" shouldNot compile
    )

    "an empty abstract class" in (
      "@tagless abstract class X" shouldNot compile
    )

    "a non-abstract class" in {
      "@tagless class X" shouldNot compile
    }

    "a trait with companion object" in {
      "@tagless trait X {def f: FS[Int]} ; object X" shouldNot compile
    }

    "an abstract class with a companion object" in {
      "@tagless trait X {def f: FS[Int]} ; object X" shouldNot compile
    }

    "a trait with any non-abstact methods of type FS" in {
      "@tagless trait X { def f(a: Char) : FS[Int] = 0 } " shouldNot compile
    }

  }

  "A @tagles trait can define derive methods by combining other basic methods" when {
    "they use a Functor[FS] instance to provide a map operation" in {
      @tagless trait X {
        def a: FS[Int]
        def b(implicit f: Functor[FS]): FS[Int] = a.map(x => x+1)
      }
      implicit object IdX extends X.Handler[Id] {
        def a: Int = 41
      }
      IdX.b shouldEqual 42
    }

    "using the Applicative instance of FS to combine operations" in {
      @tagless trait X {
        def a: FS[Int]
        def b(implicit A: Applicative[FS]): FS[Int] = (a, A.pure(1)).mapN(_+_)
      }
      object IdX extends X.Handler[Id] {
        def a: Int = 5
      }
      IdX.b shouldEqual 6
    }

    "using the Monad instance of FS to combine operations" in {
      @tagless trait X {
        def a: FS[Int]
        def b(x: Int): FS[Int]
        def c(implicit M: Monad[FS]): FS[Int] = for {
          x <- a
          y <- b(x)
          z <- M.pure(10)
        } yield y+z
      }
      object IdX extends X.Handler[Id] {
        def a: Int = 31
        def b(x: Int) = x+1
      }
      IdX.c shouldEqual 42
    }

    "mixing all of the above" in {
      @tagless trait X {
        def a: FS[Int]
        def b(i: Int)(implicit F: Functor[FS]): FS[Int] = a.map(x => x+i)
        def c(implicit A: Applicative[FS]): FS[Int] = (a,A.pure(3)).mapN(_+_)
        def d(implicit M: Monad[FS]): FS[Int] = for {
          x <- M.pure(10)
          y <- b(x)
        } yield x+y
      }
      object IdX extends X.Handler[Id] {
        def a: Int = 5
      }
      IdX.c shouldEqual 8
      IdX.d shouldEqual(25)
    }

  }

  "Tagless final algebras" should {

    "Allow a trait with an F[_] bound type param" in {
      "@tagless trait X[F[_]] { def bar(x:Int): FS[Int] }" should compile
    }

    "combine with other tagless algebras" in {

      def program[F[_] : Monad : TG1 : TG2: TG3] = {
        val a1 = TG1[F]
        val a2 = TG2[F]
        val a3 = TG3[F]
        for {
          a <- a1.x(1)
          b <- a1.y(1)
          c <- a2.x2(1)
          d <- a2.y2(1)
          e <- a3.y3(1)
        } yield a + b + c + d + e
      }
      program[Option] shouldBe Option(5)
    }

    "combine with FreeS monadic comprehensions" in {
      import freestyle.free._
      import freestyle.free.implicits._

      def program[F[_] : F1: TG1.StackSafe : TG2.StackSafe]: FreeS[F, Int] = {
        val tg1 = TG1.StackSafe[F]
        val tg2 = TG2.StackSafe[F]
        val fs = F1[F]
        val x : FreeS[F, Int] = for {
          a <- fs.a(1)
          tg2a <- tg2.x2(1)
          b <- fs.b(1)
          x <- tg1.x(1)
          tg2b <- tg2.y2(1)
          y <- tg1.y(1)
        } yield a + b + x + y + tg2a + tg2b
        x
      }
      import TG1._
      program[App.Op].interpret[Option] shouldBe Option(6)
    }

    "work with derived handlers" in {

      def program[F[_]: TG1: Monad] =
        for {
          x <- TG1[F].x(1)
          y <- TG1[F].y(2)
        } yield x + y

      type ErrorOr[A] = Either[String, A]

      implicit val fk: Option ~> ErrorOr =
        λ[Option ~> ErrorOr](_.toRight("error"))

      program[ErrorOr] shouldBe Right(3)
    }

    "allow for tagless modules" in {

      def program[F[_]: AppTagless: Monad] =
        for {
          x <- AppTagless[F].tg1.x(1)
          y <- AppTagless[F].tg2.x2(2)
        } yield x + y

      program[Option] shouldBe Some(3)
    }

  }

}

object algebras {

  @tagless
  trait TG1 {
    def x(a: Int): FS[Int]

    def y(a: Int): FS[Int]
  }

  @tagless
  trait TG2 {
    def x2(a: Int): FS[Int]

    def y2(a: Int): FS[Int]
  }

  @tagless
  trait TG3[F[_]] {
    def x3(a: Int): FS[Int]

    def y3(a: Int): FS[Int]
  }

  @free
  trait F1 {
    def a(a: Int): FS[Int]

    def b(a: Int): FS[Int]
  }

}

object handlers  {

  import algebras._

  implicit val optionHandler1: TG1.Handler[Option] = new TG1.Handler[Option] {
    def x(a: Int): Option[Int] = Some(a)

    def y(a: Int): Option[Int] = Some(a)
  }

  implicit val optionHandler2: TG2.Handler[Option] = new TG2.Handler[Option] {
    def x2(a: Int): Option[Int] = Some(a)

    def y2(a: Int): Option[Int] = Some(a)
  }

  implicit val optionHandler3: TG3.Handler[Option] = new TG3.Handler[Option] {
    def x3(a: Int): Option[Int] = Some(a)

    def y3(a: Int): Option[Int] = Some(a)
  }

  implicit val f1OptionHandler1: F1.Handler[Option] = new F1.Handler[Option] {
    def a(a: Int): Option[Int] = Some(a)

    def b(a: Int): Option[Int] = Some(a)
  }

  implicit def mIdentity[F[_]]: F ~> F = FunctionK.id[F]

}

object modules {

  import algebras._

  @_root_.freestyle.free.module trait App {
    val f1: F1
    val tg1: TG1.StackSafe
    val tg2: TG2.StackSafe
  }

  @_root_.freestyle.tagless.module trait AppTagless {
    val tg1: TG1
    val tg2: TG2
  }

}

object utils {

  import algebras._

  val iterations = 50000

  def SOProgram[F[_] : Monad : TG1](i: Int): F[Int] = for {
    j <- TG1[F].x(i + 1)
    z <- if (j < iterations) SOProgram[F](j) else Monad[F].pure(j)
  } yield z

}