package io.hydrosphere.mist.master.store

import java.nio.file.Paths

import io.hydrosphere.mist.jobs.{Action, JobDetails, JobExecutionParams}
import io.hydrosphere.mist.utils.TypeAlias._
import org.scalatest._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class H2RepoSpec extends FlatSpec with Matchers with BeforeAndAfter with BeforeAndAfterAll {

  val path = Paths.get("./target", "h2_repo")

  override def afterAll(): Unit = org.h2.tools.DeleteDbFiles.execute("./target", "h2_repo", true)

  val repo: JobRepository = H2JobsRepository(path.toString)

  after {
    repo.clear().await
  }

  it should "update" in {
    val details = fixtureJobDetails("id")
    repo.update(details)
    repo.get(details.jobId).await shouldBe Some(details)
  }

  it should "remove" in {
    val details = fixtureJobDetails("id")
    repo.remove(details.jobId)
    repo.get(details.jobId).await shouldBe None
  }

  it should "clear" in {
    (1 to 10).foreach(i => repo.update(fixtureJobDetails(s"jobId $i")).await)
    repo.clear().await
    repo.all().await.size shouldBe 0
  }

  it should "filter by status" in {
    (1 to 2).foreach(i => {
      val details = fixtureJobDetails(s"jobId $i", JobDetails.Status.Running)
      repo.update(details).await
    })
    repo.update(fixtureJobDetails("ignore")).await

    val runningJobs = repo.filteredByStatuses(List(JobDetails.Status.Running))
    runningJobs.await.size shouldBe 2
  }

  private def fixtureJobDetails(
    jobId: String,
    status: JobDetails.Status = JobDetails.Status.Initialized): JobDetails = {
    val conf = JobExecutionParams(
      path = "path",
      className = "com.yoyo.MyClass",
      namespace = "namespace",
      parameters = JobParameters("key" -> "value"),
      externalId = Some("externalId"),
      route = Some("route"),
      action = Action.Serve
    )

    JobDetails(
      configuration = conf,
      source = JobDetails.Source.Cli,
      jobId = jobId,
      status = status
    )
  }

  implicit class AwaitSyntax[A](f: => Future[A]) {
    def await: A = Await.result(f, Duration.Inf)
  }
}


