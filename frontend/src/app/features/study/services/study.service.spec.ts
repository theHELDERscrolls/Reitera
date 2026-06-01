import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { StudyService } from './study.service';
import { DueCard, StudySessionRequest, StudySessionResponse } from '@core/models/study.model';

const DUE_URL = 'http://localhost:8080/api/v1/study/due';
const SESSIONS_URL = 'http://localhost:8080/api/v1/study/sessions';

const mockDueCards: DueCard[] = [
  {
    id: 1,
    deckId: 10,
    type: 'BASIC',
    question: 'What is a signal?',
    answerJson: { answer: 'A synchronous reactive value' },
    explanation: null,
    state: 0,
  },
];

const mockSessionRequest: StudySessionRequest = {
  deckId: 10,
  categoryId: null,
  ratings: [{ cardId: 1, rating: 3 }],
};

const mockSessionResponse: StudySessionResponse = {
  deckId: 10,
  categoryId: null,
  cardsReviewed: 1,
};

describe('StudyService', () => {
  let service: StudyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StudyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDueCards()', () => {
    it('makes GET /study/due without params when no filter is provided', () => {
      service.getDueCards().subscribe();

      const req = httpMock.expectOne(DUE_URL);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys()).toHaveLength(0);

      req.flush(mockDueCards);
    });

    it('includes deckId param when provided', () => {
      service.getDueCards(7).subscribe();

      const req = httpMock.expectOne((r) => r.url === DUE_URL);
      expect(req.request.params.get('deckId')).toBe('7');
      expect(req.request.params.has('categoryId')).toBe(false);

      req.flush(mockDueCards);
    });

    it('includes categoryId param when provided', () => {
      service.getDueCards(undefined, 3).subscribe();

      const req = httpMock.expectOne((r) => r.url === DUE_URL);
      expect(req.request.params.get('categoryId')).toBe('3');
      expect(req.request.params.has('deckId')).toBe(false);

      req.flush(mockDueCards);
    });

    it('includes both params when deckId and categoryId are provided', () => {
      service.getDueCards(7, 3).subscribe();

      const req = httpMock.expectOne((r) => r.url === DUE_URL);
      expect(req.request.params.get('deckId')).toBe('7');
      expect(req.request.params.get('categoryId')).toBe('3');

      req.flush(mockDueCards);
    });

    it('returns the DueCard array from the server', () => {
      let result: DueCard[] | undefined;
      service.getDueCards().subscribe((cards) => (result = cards));

      httpMock.expectOne(DUE_URL).flush(mockDueCards);

      expect(result).toEqual(mockDueCards);
    });
  });

  describe('processSession()', () => {
    it('makes POST /study/sessions with the session request body', () => {
      service.processSession(mockSessionRequest).subscribe();

      const req = httpMock.expectOne(SESSIONS_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockSessionRequest);

      req.flush(mockSessionResponse);
    });

    it('returns the StudySessionResponse from the server', () => {
      let result: StudySessionResponse | undefined;
      service.processSession(mockSessionRequest).subscribe((res) => (result = res));

      httpMock.expectOne(SESSIONS_URL).flush(mockSessionResponse);

      expect(result).toEqual(mockSessionResponse);
    });
  });
});
