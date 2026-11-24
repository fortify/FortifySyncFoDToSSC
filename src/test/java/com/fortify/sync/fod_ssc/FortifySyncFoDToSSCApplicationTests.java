package com.fortify.sync.fod_ssc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import jakarta.ws.rs.client.Entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.SSCAttributeDefinitionAPI.SSCAttributeDefinitionHelper;
import com.fortify.client.ssc.api.SSCIssueTemplateAPI.SSCIssueTemplateHelper;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.config.IScheduleConfig;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig;
import com.fortify.sync.fod_ssc.config.SyncScansTaskConfig;
import com.fortify.sync.fod_ssc.connection.ssc.api.SSCSyncAttr;
import com.fortify.sync.fod_ssc.connection.ssc.api.SyncStatus;
import com.fortify.sync.fod_ssc.task.AbstractScheduledTask;
import com.fortify.sync.fod_ssc.task.LinkReleasesTask;
import com.fortify.sync.fod_ssc.task.SyncScansTask;
import com.fortify.util.rest.connection.RestConnection;
import com.fortify.util.rest.json.JSONMap;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest(properties = {
	"sync.connections.fod.baseUrl=https://fod.example.test",
	"sync.connections.fod.tenant=test-tenant",
	"sync.connections.ssc.baseUrl=https://ssc.example.test",
	"sync.tasks.linkReleases.cronSchedule=0 30 * * * *",
	"sync.tasks.linkReleases.ssc.autoCreateVersions.enabled=false",
	"sync.tasks.syncScans.cronSchedule=0 0 * * * *",
	"sync.tasks.syncScans.scansTempDir=${sync.home}/scans",
	"spring.quartz.auto-startup=false"
})
class FortifySyncFoDToSSCApplicationTests {
	@MockitoBean private FoDAuthenticatingRestConnection fodConnection;
	@MockitoBean private SSCAuthenticatingRestConnection sscConnection;
	@MockitoBean private SSCAttributeDefinitionHelper sscAttributeDefinitionHelper;
	@MockitoBean private SSCIssueTemplateHelper sscIssueTemplateHelper;
	@MockitoBean(reset = MockReset.NONE) private TaskScheduler scheduler;
	@Autowired private FortifySyncFoDToSSCApplication application;
	@Autowired private LinkReleasesTaskConfig linkConfig;
	@Autowired private SyncScansTaskConfig syncConfig;
	@Autowired private LinkReleasesTask linkTask;
	@Autowired private SyncScansTask syncTask;

	@Test
	void contextLoads() {
		assertEquals("test-tenant", application.fodConnectionBuilder().getTenant());
		assertEquals("ssc.example.test", application.sscConnectionBuilder().getBaseUrl().getHost());
		assertFalse(linkConfig.getSsc().getAutoCreateVersions().isEnabled());
		assertTrue(Files.isDirectory(Path.of(syncConfig.getScansTempDir())));

		ArgumentCaptor<Trigger> linkTrigger = ArgumentCaptor.forClass(Trigger.class);
		ArgumentCaptor<Trigger> syncTrigger = ArgumentCaptor.forClass(Trigger.class);
		verify(scheduler).schedule(eq(linkTask), linkTrigger.capture());
		verify(scheduler).schedule(eq(syncTask), syncTrigger.capture());
		Clock clock = Clock.fixed(Instant.parse("2026-11-24T12:01:00Z"), ZoneId.of("Europe/Amsterdam"));
		assertEquals(Instant.parse("2026-11-24T12:30:00Z"),
			linkTrigger.getValue().nextExecution(new SimpleTriggerContext(clock)));
		assertEquals(Instant.parse("2026-11-24T13:00:00Z"),
			syncTrigger.getValue().nextExecution(new SimpleTriggerContext(clock)));
	}

	@Test
	void persistedSyncStatusRemainsCompatible() throws Exception {
		String attributeName = SSCSyncAttr.FOD_SYNC_STATUS.getAttributeName();
		String stored = "{\"fodReleaseId\":\"123\",\"scanDates\":{\"static\":1700000000000},\"futureField\":true}";
		JSONMap version = new JSONMap(Map.of("attributeValuesByName",
			new JSONMap(Map.of(attributeName, stored))));
		SyncStatus status = SyncStatus.getFromApplicationVersion(version);
		assertFalse(status.isModified());
		assertSame(status, status.newIfDifferentFoDReleaseId("123"));
		assertEquals(new Date(1700000000000L), status.getScanDate("Static"));

		status.setScanDate("Dynamic", new Date(1700000001000L));
		String serialized = (String) status.asAttributesMap().getFirst(attributeName);
		var json = new ObjectMapper().readTree(serialized);
		assertEquals("123", json.get("fodReleaseId").asText());
		assertEquals(1700000000000L, json.path("scanDates").path("static").asLong());
		assertEquals(1700000001000L, json.path("scanDates").path("dynamic").asLong());
		assertFalse(json.has("modified"));
	}

	@Test
	void clientReadsAndWritesNestedJsonWithJakartaProvider() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/echo", exchange -> {
			try (exchange) {
				byte[] body = exchange.getRequestBody().readAllBytes();
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		});
		server.start();
		try (RestConnection connection = RestConnection.builder()
				.baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).multiThreaded(true).build()) {
			JSONMap payload = new JSONMap(Map.of("releaseId", 123,
				"application", new JSONMap(Map.of("applicationName", "example"))));
			JSONMap result = connection.executeRequest("POST", connection.getBaseResource().path("echo"),
				Entity.json(payload), JSONMap.class);
			assertEquals(123, result.get("releaseId", Integer.class));
			assertEquals("example", result.get("application", JSONMap.class).get("applicationName", String.class));
		} finally {
			server.stop(0);
		}
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "-"})
	void disabledScheduleDoesNotRegisterTask(String cron) {
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		var task = scheduledTask(cron, taskScheduler);
		task.postConstruct();
		verifyNoInteractions(taskScheduler);
	}

	@Test
	void invalidScheduleFailsDuringInitialization() {
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		var task = scheduledTask("not a cron expression", taskScheduler);
		assertThrows(IllegalArgumentException.class, task::postConstruct);
		verifyNoInteractions(taskScheduler);
	}

	private AbstractScheduledTask<IScheduleConfig> scheduledTask(String cron, TaskScheduler taskScheduler) {
		var task = new AbstractScheduledTask<IScheduleConfig>() {
			@Override
			protected void runTask() {}

			@Override
			protected IScheduleConfig getConfig() {
				return () -> cron;
			}
		};
		ReflectionTestUtils.setField(task, "scheduler", taskScheduler);
		return task;
	}
}
