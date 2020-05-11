package com.freebds.backend.service;

import com.freebds.backend.common.web.freeSearch.resources.FreeSearchResource;
import com.freebds.backend.common.web.freeSearch.resources.FreeSearchType;
import com.freebds.backend.common.web.context.resources.ContextResource;
import com.freebds.backend.exception.CollectionItemNotFoundException;
import com.freebds.backend.exception.FreeBdsApiException;
import com.freebds.backend.mapper.AuthorMapper;
import com.freebds.backend.mapper.SerieMapper;
import com.freebds.backend.model.Author;
import com.freebds.backend.model.GraphicNovel;
import com.freebds.backend.model.Serie;
import com.freebds.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class FreeSearchServiceImpl implements FreeSearchService {

    private final GraphicNovelService graphicNovelService;

    private final SerieRepository serieRepository;
    private final GraphicNovelRepository graphicNovelRepository;
    private final AuthorRepository authorRepository;

    private final LibrarySerieContentRepository librarySerieContentRepository;
    private final LibraryContentRepository libraryContentRepository;

    /**
     * Retrieve all existing series / graphic novels / authors by multiple criteria
     * @param pageable the page to get
     * @param libraryId the library id to get
     * @param serieTitle the serie title to get
     * @param serieExternalId the serie external id to get
     * @param categories the serie category to get
     * @param status the serie status to get
     * @param origin the serie origin to get
     * @param language the serie language to get
     * @param graphicNovelTitle the graphic novel title to get
     * @param graphicNovelExternalId the graphic novel external id to get
     * @param publisher the graphic novel publisher to get
     * @param collection the graphic novel collection to get
     * @param isbn the graphic novel ISBN to get
     * @param publicationDateFrom the graphic novel publication date from to get
     * @param publicationDateTo the graphic novel publication date to to get
     * @param lastname the author lastname to get
     * @param firstname the author firstname to get
     * @param nickname the author nickname to get
     * @param authorExternalId the author external id to get
     * @param nationality the author nationality to get
     * @return a page of filtered series / graphic novels / authors
     * @throws CollectionItemNotFoundException in case of invalid selected value requests
     */
    @Override
    public FreeSearchResource search(
            // Common parameters
            Pageable pageable, ContextResource contextResource, String freeSearchTypeRequest, Long libraryId,
            // Serie filters parameters
            String serieTitle, String serieExternalId, String categories, String status, String origin, String language,
            // Graphic novel filters parameters
            String graphicNovelTitle, String graphicNovelExternalId, String publisher, String collection, String isbn, Date publicationDateFrom, Date publicationDateTo,
            // Author filters parameters
            String lastname, String firstname, String nickname, String authorExternalId, String nationality ) throws CollectionItemNotFoundException, FreeBdsApiException {

        // Check parameters validity
        //--------------------------
        // Search type validity
        FreeSearchType freeSearchType = FreeSearchType.UNDEFINED;
        if(freeSearchTypeRequest.equals(FreeSearchType.SERIES.toString()))
            freeSearchType = FreeSearchType.SERIES;

        if(freeSearchTypeRequest.equals(FreeSearchType.GRAPHIC_NOVELS.toString()))
            freeSearchType = FreeSearchType.GRAPHIC_NOVELS;

        if(freeSearchTypeRequest.equals(FreeSearchType.AUTHORS.toString()))
            freeSearchType = FreeSearchType.AUTHORS;

        if(freeSearchTypeRequest.equals(FreeSearchType.UNDEFINED.toString())) {
            throw new FreeBdsApiException(
                    "Type de recherche invalide",
                    freeSearchTypeRequest
            );
        }

        // Force null value for empty parameters
        serieTitle = (serieTitle == "" ? null : serieTitle);
        serieExternalId = (serieExternalId == "" ? null : serieExternalId);
        categories = (categories == "" ? null : categories);
        status = (status == "" ? null : status);
        origin = (origin == "" ? null : origin);
        language = (language == "" ? null : language);
        graphicNovelTitle = (graphicNovelTitle == "" ? null : graphicNovelTitle);
        graphicNovelExternalId = (graphicNovelExternalId == "" ? null : graphicNovelExternalId);
        publisher = (publisher == "" ? null : publisher);
        collection = (collection == "" ? null : collection);
        isbn = (isbn == "" ? null : isbn);
        lastname = (lastname == "" ? null : lastname);
        firstname = (firstname == "" ? null : firstname);
        nickname = (nickname == "" ? null : nickname);
        authorExternalId = (authorExternalId == "" ? null : authorExternalId);
        nationality = (nationality == "" ? null : nationality);

        // Set all optional contains searches to lower case for SQL compliance research
        serieTitle = (serieTitle == null ? null : serieTitle.toLowerCase());
        graphicNovelTitle = (graphicNovelTitle == null ? null : graphicNovelTitle.toLowerCase());
        publisher = (publisher == null ? null : publisher.toLowerCase());
        collection = (collection == null ? null : collection.toLowerCase());
        lastname = (lastname == null ? null : lastname.toLowerCase());
        firstname = (firstname == null ? null : firstname.toLowerCase());
        nickname = (nickname == null ? null : nickname.toLowerCase());

        // Check if selected values from combo box lists exist in DB
        isItemExists(categories, serieRepository.findDistinctCategories(), "Categories", true);
        isItemExists(status, serieRepository.findDistinctStatus(), "Status", true);
        isItemExists(origin, serieRepository.findDistinctOrigins(), "Origin", true);
        isItemExists(language, serieRepository.findDistinctLanguages(), "Languages", true);
        isItemExists(nationality, authorRepository.findDistinctNationalities(), "Nationalities", true);

        // Search and build the resource result object
        FreeSearchResource freeSearch = new FreeSearchResource();
        // Search series in the referential
        if(contextResource.getContext().equals("referential") && freeSearchType.equals(FreeSearchType.SERIES)) {
            Page<Serie> series = this.serieRepository
                    .findBySearchFilters(
                            pageable,
                            serieTitle, serieExternalId, categories, status, origin, language,
                            graphicNovelTitle, graphicNovelExternalId, publisher, collection, isbn, publicationDateFrom, publicationDateTo,
                            lastname, firstname, nickname, authorExternalId, nationality
                    );
            freeSearch.setSeries(series.map(serie -> SerieMapper.INSTANCE.toResource(serie)));
        }

        // Search graphic novels in the referential
        if(contextResource.getContext().equals("referential") && freeSearchType.equals(FreeSearchType.GRAPHIC_NOVELS)) {
            Page<GraphicNovel> graphicNovels = this.graphicNovelRepository
                    .findBySearchFilters(
                            pageable,
                            serieTitle, serieExternalId, categories, status, origin, language,
                            graphicNovelTitle, graphicNovelExternalId, publisher, collection, isbn, publicationDateFrom, publicationDateTo,
                            lastname, firstname, nickname, authorExternalId, nationality
                    );
            // Add library infos
            freeSearch.setGraphicNovels(graphicNovels.map(graphicNovel -> this.graphicNovelService.addGraphicNovelResourceDependencies(contextResource, graphicNovel)));
        }

        // Search authors in te referential
        if(contextResource.getContext().equals("referential") && freeSearchType.equals(FreeSearchType.AUTHORS)) {
            Page<Author> authors = this.authorRepository
                    .findBySearchFilters(
                            pageable,
                            serieTitle, serieExternalId, categories, status, origin, language,
                            graphicNovelTitle, graphicNovelExternalId, publisher, collection, isbn, publicationDateFrom, publicationDateTo,
                            lastname, firstname, nickname, authorExternalId, nationality
                    );
            freeSearch.setAuthors(authors.map(author -> AuthorMapper.INSTANCE.toResource(author)));
        }

        // Search series in the library
        if(contextResource.getContext().equals("library") && freeSearchType.equals(FreeSearchType.SERIES)) {
            Page<Serie> series = this.librarySerieContentRepository
                    .findBySearchFilters(
                            pageable, libraryId,
                            serieTitle, serieExternalId, categories, status, origin, language,
                            graphicNovelTitle, graphicNovelExternalId, publisher, collection, isbn, publicationDateFrom, publicationDateTo,
                            lastname, firstname, nickname, authorExternalId, nationality
                    );
            freeSearch.setSeries(series.map(serie -> SerieMapper.INSTANCE.toResource(serie)));
        }

        // Search graphic novels in the library
        if(contextResource.getContext().equals("library") && freeSearchType.equals(FreeSearchType.GRAPHIC_NOVELS)) {
            Page<GraphicNovel> graphicNovels = this.libraryContentRepository
                    .findBySearchFilters(
                            pageable, libraryId,
                            serieTitle, serieExternalId, categories, status, origin, language,
                            graphicNovelTitle, graphicNovelExternalId, publisher, collection, isbn, publicationDateFrom, publicationDateTo,
                            lastname, firstname, nickname, authorExternalId, nationality
                    );
            // Add library infos
            freeSearch.setGraphicNovels(graphicNovels.map(graphicNovel -> this.graphicNovelService.addGraphicNovelResourceDependencies(contextResource, graphicNovel)));
        }

        // Search authors in the library
        if(contextResource.getContext().equals("library") && freeSearchType.equals(FreeSearchType.AUTHORS)) {
            Page<Author> authors = this.authorRepository
                    .findInLibraryBySearchFilters(
                            pageable, libraryId,
                            serieTitle, serieExternalId, categories, status, origin, language,
                            graphicNovelTitle, graphicNovelExternalId, publisher, collection, isbn, publicationDateFrom, publicationDateTo,
                            lastname, firstname, nickname, authorExternalId, nationality
                    );
            freeSearch.setAuthors(authors.map(author -> AuthorMapper.INSTANCE.toResource(author)));
        }

        return freeSearch;
    }

    /**
     * Utility method to check validaty of a select on a combo box
     * @param item the item to check
     * @param collection the collection to scan
     * @param collectionName the collection's name for error message
     * @param isThrowable throws an exception if true
     * @return
     */
    public static boolean isItemExists(String item, List<String> collection, String collectionName, boolean isThrowable) {
        // Check if selected values from combo box lists exist in DB
        boolean result = true;
        if(item != null ) {
            if(! collection.contains(item)) {
                result = false;
            }
        }

        if(isThrowable && result == false) {
            throw new CollectionItemNotFoundException(item, collectionName);
        }

        return result;
    }

}