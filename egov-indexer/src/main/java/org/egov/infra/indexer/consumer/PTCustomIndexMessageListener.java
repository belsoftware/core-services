package org.egov.infra.indexer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.egov.infra.indexer.custom.pt.PTCustomDecorator;
import org.egov.infra.indexer.custom.pt.PropertyAssessmentRequest;
import org.egov.infra.indexer.custom.pt.PropertyRequest;
import org.egov.infra.indexer.custom.pt.PropertyResponse;
import org.egov.infra.indexer.service.IndexerService;
import org.egov.infra.indexer.util.IndexerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PTCustomIndexMessageListener implements MessageListener<String, String> {

	@Autowired
	private IndexerService indexerService;

	@Autowired
	private IndexerUtils indexerUtils;

	@Autowired
	private PTCustomDecorator ptCustomDecorator;

	@Value("${egov.indexer.pt.update.topic.name}")
	private String ptUpdateTopic;

	@Override
	/**
	 * Messages listener which acts as consumer. This message listener is injected
	 * inside a kafkaContainer. This consumer is a start point to the following
	 * index jobs: 1. Re-index 2. Legacy Index 3. PGR custom index 4. PT custom
	 * index 5. Core indexing
	 */
	public void onMessage(ConsumerRecord<String, String> data) {
		log.info("Topic: " + data.topic());
		ObjectMapper mapper = indexerUtils.getObjectMapper();
		try {
			PropertyAssessmentRequest propertyAssessmentRequest = mapper.readValue(data.value(), PropertyAssessmentRequest.class);
			PropertyResponse propertyResponse = ptCustomDecorator.dataTransformForPTUpdate(propertyAssessmentRequest);
			indexerService.esIndexer(data.topic(), mapper.writeValueAsString(propertyResponse));
		} catch (Exception e) {
			log.error("Couldn't parse ptindex request: ", e);
		}
	}

}
