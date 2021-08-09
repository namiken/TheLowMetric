package jp.thelow.metric;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.elastic.ElasticConfig;
import io.micrometer.elastic.ElasticMeterRegistry;
import lombok.Getter;

public class TheLowMetric extends JavaPlugin implements Listener {

  public static TheLowMetric instance;

  public static Logger logger;

  @Getter
  private static MeterRegistry registry;

  private JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();

  @Override
  public void onEnable() {

    TheLowMetric.instance = this;
    this.getConfig().options().copyDefaults(true);
    saveDefaultConfig();

    getServer().getPluginManager().registerEvents(this, this);
    logger = getLogger();

    Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new TpsGetter(), 100L, 1L);

    loadMicrometer();
  }

  private void loadMicrometer() {
    //Elastic Searchの設定をロード
    ConfigurationSection section = getConfig().getConfigurationSection("elastic-search");
    if (section.getBoolean("enable")) {
      ElasticConfig elasticConfig = new ElasticConfig() {
        @Override
        public String get(String k) {
          return null;
        }

        @Override
        public String host() {
          return section.getString("host");
        }

        @Override
        public Duration step() {
          return Duration.ofSeconds(section.getInt("step-secound", 20));
        }

        @Override
        public String indexDateFormat() {
          return "yyyy-MM-dd";
        }

        @Override
        public String index() {
          return section.getString("index");
        }

        @Override
        public String password() {
          return section.getString("password");
        }

      };
      registry = new ElasticMeterRegistry(elasticConfig, Clock.SYSTEM);

      List<Map<?, ?>> commonTag = section.getMapList("common-tag");
      if (commonTag != null) {
        List<Tag> tagList = new ArrayList<>();
        for (Map<?, ?> map : commonTag) {
          List<Tag> tag = map.entrySet().stream().map(e -> Tag.of(e.getKey().toString(), e.getValue().toString()))
              .collect(Collectors.toList());
          tagList.addAll(tag);
        }
        registry.config().commonTags(tagList);
      }

      logger.info("Elastic Searchの設定が完了しました。");
    }

    if (registry != null) {
      Metrics.addRegistry(registry);

      //JMVのメトリクスを収集
      new JvmMemoryMetrics().bindTo(registry);
      new FileDescriptorMetrics().bindTo(registry);
      new ProcessorMetrics().bindTo(registry);
      jvmGcMetrics.bindTo(registry);

      //プレイヤー数
      Gauge
          .builder("players", () -> Bukkit.getOnlinePlayers().size())
          .register(registry);

      //TPS
      Gauge
          .builder("tps", () -> TpsGetter.getTPS())
          .register(registry);
    } else {
      registry = new SimpleMeterRegistry();
    }

  }

  @Override
  public void onDisable() {
    jvmGcMetrics.close();
    registry.close();
  }

  public static Logger logger() {
    return logger;
  }

}
