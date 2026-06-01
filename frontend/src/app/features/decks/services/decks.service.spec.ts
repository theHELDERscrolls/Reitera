import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { DecksService } from './decks.service';
import { DeckRequest, DeckResponse } from '@core/models/deck.model';
import { Category } from '@core/models/category.model';
import { Page } from '@core/models/page.model';

const BASE_URL = 'http://localhost:8080/api/v1/decks';
const CATEGORIES_URL = 'http://localhost:8080/api/v1/categories';

const mockDeckRequest: DeckRequest = {
  title: 'Test deck',
  description: 'Test description',
  categoryId: null,
  categoryName: null,
};

const mockDeckResponse: DeckResponse = {
  id: 1,
  title: 'Test deck',
  description: 'Test description',
  authorName: 'user',
  categoryId: null,
  categoryName: null,
  createdAt: '2024-01-01T00:00:00',
  updatedAt: '2024-01-01T00:00:00',
  newCount: 0,
  dueCount: 0,
  relearningCount: 0,
};

const mockPage: Page<DeckResponse> = {
  content: [mockDeckResponse],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 12,
  first: true,
  last: true,
};

describe('DecksService', () => {
  let service: DecksService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DecksService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDecks()', () => {
    it('makes GET /decks with default params (page=0, size=12, sort=createdAt,desc)', () => {
      service.getDecks().subscribe();

      const req = httpMock.expectOne((r) => r.url === BASE_URL);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('12');
      expect(req.request.params.get('sort')).toBe('createdAt,desc');

      req.flush(mockPage);
    });

    it('includes categoryId param when provided', () => {
      service.getDecks(0, 12, 5).subscribe();

      const req = httpMock.expectOne((r) => r.url === BASE_URL);
      expect(req.request.params.get('categoryId')).toBe('5');

      req.flush(mockPage);
    });

    it('does NOT include categoryId when not provided', () => {
      service.getDecks().subscribe();

      const req = httpMock.expectOne((r) => r.url === BASE_URL);
      expect(req.request.params.has('categoryId')).toBe(false);

      req.flush(mockPage);
    });

    it('returns the Page<DeckResponse> from the server', () => {
      let result: Page<DeckResponse> | undefined;
      service.getDecks().subscribe((page) => (result = page));

      httpMock.expectOne((r) => r.url === BASE_URL).flush(mockPage);

      expect(result).toEqual(mockPage);
    });
  });

  describe('getCategories()', () => {
    const mockCategories: Category[] = [{ id: 1, name: 'Languages', description: 'Languages' }];

    it('makes GET /categories', () => {
      service.getCategories().subscribe();

      const req = httpMock.expectOne(CATEGORIES_URL);
      expect(req.request.method).toBe('GET');
      req.flush(mockCategories);
    });

    it('returns the array of categories', () => {
      let result: Category[] | undefined;
      service.getCategories().subscribe((cats) => (result = cats));

      httpMock.expectOne(CATEGORIES_URL).flush(mockCategories);

      expect(result).toEqual(mockCategories);
    });
  });

  describe('createDeck()', () => {
    it('makes POST /decks with deck data', () => {
      service.createDeck(mockDeckRequest).subscribe();

      const req = httpMock.expectOne(BASE_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockDeckRequest);

      req.flush(mockDeckResponse);
    });
  });

  describe('updateDeck()', () => {
    it('makes PUT /decks/:id with updated data', () => {
      service.updateDeck(3, mockDeckRequest).subscribe();

      const req = httpMock.expectOne(`${BASE_URL}/3`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(mockDeckRequest);

      req.flush(mockDeckResponse);
    });
  });

  describe('deleteDeck()', () => {
    it('makes DELETE /decks/:id', () => {
      service.deleteDeck(3).subscribe();

      const req = httpMock.expectOne(`${BASE_URL}/3`);
      expect(req.request.method).toBe('DELETE');

      req.flush(null);
    });
  });
});
