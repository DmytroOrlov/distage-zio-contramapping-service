package example

import zio.console._

object Main extends zio.App {
  val app = for {
    _ <- putStrLn("hello")
  } yield ()

  def run(args: List[String]) =
    app.fold(_ => 1, _ => 0)
}

import cats.tagless._

@finalAlg
@autoFunctorK
@autoSemigroupalK
@autoProductNK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]

  def divide(dividend: Float, divisor: Float): F[Float]
}

import cats.Contravariant
import distage._
import izumi.distage.model.providers.ProviderMagnet
import izumi.distage.constructors.TraitConstructor
import zio._

object App extends zio.App {

  trait Dependee[-R] {
    def x(y: String): URIO[R, Int]
  }

  trait Depender[-R] {
    def y: URIO[R, String]
  }

  implicit val contra1: Contravariant[Dependee] = new Contravariant[Dependee] {
    def contramap[A, B](fa: Dependee[A])(f: B => A): Dependee[B] = new Dependee[B] {
      def x(y: String) = fa.x(y).provideSome(f)
    }
  }
  implicit val contra2: Contravariant[Depender] = new Contravariant[Depender] {
    def contramap[A, B](fa: Depender[A])(f: B => A): Depender[B] = new Depender[B] {
      def y = fa.y.provideSome(f)
    }
  }

  trait DependeeR {
    def dependee: Dependee[Any]
  }

  trait DependerR {
    def depender: Depender[Any]
  }

  object dependee extends Dependee[DependeeR] {
    def x(y: String) = ZIO.accessM(_.dependee.x(y))
  }

  object depender extends Depender[DependerR] {
    def y = ZIO.accessM(_.depender.y)
  }

  def run(args: List[String]) = {
    // cycle
    object dependerImpl extends Depender[DependeeR] {
      def y: URIO[DependeeR, String] = dependee.x("hello").map(_.toString)
    }
    object dependeeImpl extends Dependee[DependerR] {
      def x(y: String): URIO[DependerR, Int] = if (y == "hello") UIO(5) else depender.y.map(y.length + _.length)
    }
    def fullfill[R: Tag : TraitConstructor, M[_] : TagK : Contravariant](m: M[R]): ProviderMagnet[M[Any]] = {
      TraitConstructor[R].provider.map(r => Contravariant[M].contramap(m)(_ => r))
    }

    val module = new ModuleDef {
      make[Depender[Any]].from(fullfill(dependerImpl))
      make[Dependee[Any]].from(fullfill(dependeeImpl))
    }

    Injector()
      .produceF[Task](module, GCMode.NoGC).use(_ run TraitConstructor[DependeeR].provider.map {
      (for {
        r <- dependee.x("zxc")
        _ <- Task(println(s"result: $r"))
      } yield ()).provide(_)
    }).fold(_ => 1, _ => 0)
  }
}
