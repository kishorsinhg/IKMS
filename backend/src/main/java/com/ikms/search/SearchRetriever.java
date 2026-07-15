package com.ikms.search;

import java.util.List;

public interface SearchRetriever {

  List<SearchEvidenceCandidate> retrieve(SearchQueryContext context);
}
