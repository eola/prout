package lib

import com.madgag.git._
import com.madgag.scalagithub.commands.CreateRepo
import com.madgag.scalagithub.model.Repo
import com.madgag.time.Implicits._
import lib.gitgithub._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.scalatest.BeforeAndAfterAll

import java.nio.file.Files.createTempDirectory
import java.time.Duration.ofMinutes
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

trait TestRepoCreation extends Helpers with BeforeAndAfterAll {

  val testRepoNamePrefix: String = s"prout-test-${getClass.getSimpleName}-"

  def isTestRepo(repo: Repo) =
    repo.name.startsWith(testRepoNamePrefix) && repo.created_at.toInstant.age() > ofMinutes(10)

  override def beforeAll(): Unit = {
    val oldRepos = github.listRepos("updated", "desc").all().futureValue.filter(isTestRepo)
    Future.traverse(oldRepos)(_.delete())
  }

  def createTestRepo(fileName: String): Repo = {
    val cr = CreateRepo(
      name = testRepoNamePrefix + System.currentTimeMillis().toString,
      `private` = false
    )
    val testRepoId = github.createRepo(cr).futureValue.repoId

    val localGitRepo = test.unpackRepo(fileName)

    val testGithubRepo = eventually { github.getRepo(testRepoId).futureValue }

    val config = localGitRepo.getConfig
    config.setString("remote", "origin", "url", testGithubRepo.clone_url)
    config.save()

    val defaultBranchName = testGithubRepo.default_branch
    if (Option(localGitRepo.findRef(defaultBranchName)).isEmpty) {
      println(s"Going to create a '$defaultBranchName' branch")
      localGitRepo.git.branchCreate().setName(defaultBranchName).setStartPoint("HEAD").call()
    }

    val pushResults =
      localGitRepo.git.push.setCredentialsProvider(githubCredentials.git).setPushTags().setPushAll().call()

    forAll (pushResults.asScala) { pushResult =>
      all (pushResult.getRemoteUpdates.asScala.map(_.getStatus)) must be(RemoteRefUpdate.Status.OK)
    }

    eventually {
      whenReady(testGithubRepo.refs.list().all()) { _ must not be empty }
    }

    val clonedRepo = eventually {
       Git.cloneRepository().setBare(true).setURI(testGithubRepo.clone_url)
         .setDirectory(createTempDirectory("prout-test-repo").toFile).call()
    }
    require(clonedRepo.getRepository.findRef(defaultBranchName).getObjectId == localGitRepo.resolve("HEAD"))

    testGithubRepo
  }
}