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
      val y = fa.y.provideSome(f)
    }
  }

  object zioenv {

    trait DependeeEnv {
      def dependee: Dependee[Any]
    }

    trait DependerEnv {
      def depender: Depender[Any]
    }

    object dependee extends Dependee[DependeeEnv] {
      def x(y: String) = ZIO.accessM(_.dependee.x(y))
    }

    object depender extends Depender[DependerEnv] {
      val y = ZIO.accessM(_.depender.y)
    }

  }

  import example.App.zioenv._

  def run(args: List[String]) = {
    // cycle
    object dependerImpl extends Depender[DependeeEnv] {
      val y: URIO[DependeeEnv, String] = dependee.x("hello").map(_.toString)
    }
    object dependeeImpl extends Dependee[DependerEnv] {
      def x(y: String): URIO[DependerEnv, Int] = if (y == "hello") UIO(5) else depender.y.map(y.length + _.length)
    }
    def fullfill[R: Tag : TraitConstructor, M[_] : TagK : Contravariant](m: M[R]): ProviderMagnet[M[Any]] = {
      TraitConstructor[R].provider.map(r => Contravariant[M].contramap(m)(_ => r))
    }

    val module = new ModuleDef {
      make[Depender[Any]].from(fullfill(dependerImpl))
      make[Dependee[Any]].from(fullfill(dependeeImpl))
    }
    Injector()
      .produceF[Task](module, GCMode.NoGC).use(_ run TraitConstructor[DependeeEnv].provider.map {
      (for {
        r <- dependee.x("zxc")
        _ <- Task(println(s"result: $r"))
      } yield ()).provide(_)
    }).fold(_ => 1, _ => 0)
  }
}
