package io.confluent.connect.solr.sink.config;

import io.confluent.connect.solr.sink.solr.SolrInputDocumentHandlerFactory;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.errors.ConnectException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SolrInputDocumentHandlerConfig<T extends SolrSinkTopicConfig> extends AbstractConfig {
  static ConfigDef config = baseConfigDef();

  public static ConfigDef baseConfigDef() {
    return new ConfigDef()
        ;
  }

  final Map<String, T> topicConfigLookup;

  public SolrInputDocumentHandlerConfig(Map<String, String> props) {
    this(config, props);
  }

  protected SolrInputDocumentHandlerConfig(ConfigDef subclassConfigDef, Map<String, String> props) {
    super(subclassConfigDef, props);
    this.topicConfigLookup = loadSolr();
  }

  /**
   * Method is used to create a SolrSinkTopicConfig specific to the SolrInputDocumentHandler.
   * @param props
   * @return
   */
  protected T createTopicConfig(Map<String, String> props) {
    throw new UnsupportedOperationException();
  }

  /**
   * Method is used to cycle through all of the potential topic specific configurations
   * and create a lookup table by the topic.
   * @return
   */
  private Map<String, T> loadSolr(){
    Pattern pattern = Pattern.compile("^solr\\d+\\.");

    Set<String> prefixes = new LinkedHashSet<>();

    Map<String, String> input = originalsStrings();

    for(Map.Entry<String, String> kvp:input.entrySet()) {
      Matcher matcher = pattern.matcher(kvp.getKey());

      if(!matcher.find()){
        continue;
      }

      String prefix = matcher.group(0);
      prefixes.add(prefix);
    }

    Map<String, T> topicToConfig = new LinkedHashMap<>();

    for(String prefix:prefixes){
      Map<String, Object> prefixedOriginals = this.originalsWithPrefix(prefix);
      Map<String, String> stringOriginals = new LinkedHashMap<>();
      for(Map.Entry<String, Object> kvp:prefixedOriginals.entrySet()){
        stringOriginals.put(kvp.getKey(), kvp.getValue().toString());
      }
      T topicConfig = createTopicConfig(stringOriginals);
      topicToConfig.put(topicConfig.getTopic(), topicConfig);
    }
    return topicToConfig;
  }

  /**
   * Method is used to return the config object for the requested topic.
   * @param topic
   * @return
   * @exception ConnectException if the topic is not configured.
   */
  public T getTopicConfig(String topic) {
    T config = this.topicConfigLookup.get(topic);
    if(null==config){
      throw new ConnectException(
          String.format(
              "Could not find configuration for topic '%s'",
              topic
          )
      );
    }
    return config;
  }
}
