package jp.thelow.metric;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;

public class TheLowMetricApi {

  /**
   * カウンターを加算する。
   *
   * @param counterName カウンター名
   * @param tags tag
   */
  public static void incrementCounter(String counterName, String... tags) {
    Counter.builder(counterName).tags(tags).register(TheLowMetric.getRegistry()).increment();
  }

  /**
   * 時間を記録する。
   *
   * @param timerName カウンター名
   * @param amount 時間
   * @param unit 時間の単位
   * @param tags tag tag
   */
  public static void recordTimer(String timerName, long amount, TimeUnit unit, String... tags) {
    Timer.builder(timerName).tags(tags).register(TheLowMetric.getRegistry()).record(amount, unit);
  }

  /**
   * Gaugeを作成する。
   *
   * @param timerName Gauge名
   * @param supplier 値の生成元
   * @param tags tag tag
   */
  public static void registerGauge(String timerName, Supplier<Number> supplier, String... tags) {
    Gauge.builder(timerName, supplier).tags(tags).register(TheLowMetric.getRegistry());
  }

}
