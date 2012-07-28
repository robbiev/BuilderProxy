/*
 * Copyright (C) 2008 Robbie Vanbrabant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.garbagecollected.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.garbagecollected.util.Example.ManualBuilder;

import com.googlecode.gchartjava.AxisInfo;
import com.googlecode.gchartjava.Color;
import com.googlecode.gchartjava.Data;
import com.googlecode.gchartjava.Line;
import com.googlecode.gchartjava.LineChart;

/** Comparing performance to regular builders. */
public class Benchmark {
  private static long proxy(int count) {
    long start = System.currentTimeMillis();
    int orig = count;
    while (count-->0) {
      Example
      .builder("Mandatory!")
      .optional1(35)
      .optional2('A')
      .build()
      .toString();
    }
    
    long elapsed = System.currentTimeMillis() - start;
    System.out.printf("%10s%,13d%8d%1s%n", "Proxy", orig, 
                      elapsed, "ms");
    return elapsed;
  }

  private static long noProxy(int count) {
    long start = System.currentTimeMillis();
    int orig = count;
    while (count-->0) {
      new ManualBuilder("Mandatory!")
      .optional1(35)
      .optional2('A')
      .build()
      .toString();
    }
    
    long elapsed = System.currentTimeMillis() - start;
    System.out.printf("%10s%,13d%8d%1s%n", "Manual", orig, 
                      elapsed, "ms");
    return elapsed;
  }
  
  public static void main(String[] args) {
    List<Integer> multipliers = Arrays.asList(3, 5, 10, 20, 40, 80, 150);
    float[] proxyX = new float[multipliers.size()];
    float[] noProxyX = new float[multipliers.size()];
    float totalProxy = 0F;
    float totalNoProxy = 0F;
    int counter = 0;
    
    for (int n : multipliers) {
      int number = n * 1000;
      proxyX[counter] = (float)proxy(number);
      totalProxy+=proxyX[counter];
      noProxyX[counter++] = (float)noProxy(number);
      totalNoProxy+= noProxyX[counter-1];
    }
    totalNoProxy = 5000;
    totalProxy = 5000;
    Data proxyXData = new Data(toPercentages(proxyX, totalProxy));
    Data noProxyXData = new Data(toPercentages(noProxyX, totalNoProxy));
    
    Line proxyLine = new Line(proxyXData);
    proxyLine.setLegend("Proxy Builder");
    proxyLine.setColor(Color.RED);
    
    Line noProxyLine = new Line(noProxyXData);
    noProxyLine.setLegend("Manual Builder");
    noProxyLine.setColor(Color.BLUE);
    
    LineChart chart = new LineChart(proxyLine, noProxyLine);
    
    chart.addYAxisInfo(new AxisInfo("","1 sec","2 sec","3 sec", "4 sec", "5 sec"));
    
    List<String> xLabels = new LinkedList<String>();
    for(Integer i : multipliers) {
      xLabels.add(String.format(Locale.US, "%,d", i));
    }
    chart.addXAxisInfo(new AxisInfo(xLabels));
    chart.addXAxisInfo(new AxisInfo("","","","","","","Objects Created"));
    
    chart.setTitle("BuilderProxy Benchmark on JDK 6");
    chart.setSize(400, 300);
    System.out.println(chart.createURLString());
    
  }
  
  static float[] toPercentages(float[] values, float total) {
    for (int i = 0; i < values.length; i++) {
      values[i] = (int)(values[i]/(total/100));
    }
    return values;
  }
}
