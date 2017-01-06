/**
 * ChatNoir 2 Web frontend JavaScript.
 *
 * Copyright (C) 2014 Webis Group @ Bauhaus University Weimar
 * Main Contributor: Janek Bevendorff
 */

(function () {
'use strict';

// --------------------------------------------------------------------
// ChatNoir namespace
// --------------------------------------------------------------------

var ChatNoir = {};
$.fn.ChatNoir = {};

// --------------------------------------------------------------------
// Basic onReady page transformations.
// --------------------------------------------------------------------

$(document).ready(function () {

    $('.index-page #SearchInput').focus();

    var modal = $('#ExplanationModal');
    modal.bootstrapExplanationModal('#SearchResults .meta .result-explain a');
    if (ChatNoir.isExplainMode() && 0 === window.location.hash.indexOf('#result-')) {
        modal.toggleExplanationModal();
    }

    // order grouped search results in not more than two rows
    var resultsList = $('#SearchResults');
    var odd = true;
    resultsList.find('.search-result:not(.grouped)').nextUntil('.search-result:not(.grouped)').each(function() {
        if (!$(this).prev().hasClass('grouped')) {
            odd = true;
        }

        if (odd) {
            $(this).css({ 'clear' : 'left' });
        }
        odd = !odd;
    });
});

// --------------------------------------------------------------------
// Explanation Modal
// --------------------------------------------------------------------

ChatNoir.isExplainMode = function () {
    return null !== $.urlParam('explain');
};

ChatNoir.renderExplanationTree = function (explanation) {
    var htmlString = '<ul>';
    for (var i = 0; i < explanation.length; ++i) {
        if (explanation[i] instanceof Array) {
            htmlString += ChatNoir.renderExplanationTree(explanation[i]);
        } else if (explanation[i] instanceof Object) {
            for (var j in explanation[i]) {
                htmlString += '<li><span class="key text-primary">' + j + '</span> ' +
                '<span class="text-muted"> = </span><span class="value">' + explanation[i][j] + '</span>';
            }
        }
    }
    htmlString += '</ul>';

    return htmlString;
};

$.fn.bootstrapExplanationModal = function (clickElementSelector) {
    var self = this;

    if (ChatNoir.isExplainMode()) {
        $(clickElementSelector).on('click', function (e) {
            var href = $(this).attr('href');
            window.location.hash = href.substr(href.lastIndexOf('#'));
            e.preventDefault();
            self.toggleExplanationModal();
        });

        this.on('shown.bs.modal', function () {
            self.adjustExplanationPaneWidth();
        });
    }
};

$.fn.toggleExplanationModal = function () {
    var hash = window.location.hash;
    var trecId = hash.substr(8);
    var jsonExplanation = $(hash).data('explanation');
    this.find('.modal-title').html('Explain search results');
    this.addExplanationTree(jsonExplanation, trecId);
    this.modal('show');
};

$.fn.addExplanationTree = function (jsonExplanation, trecId) {
    var body = this.find('.modal-body > div');
    body.find('#explanation-' + trecId).remove();
    this.adjustExplanationPaneWidth();

    var origHtml = body.html();
    if ('&nbsp;' === origHtml) {
        origHtml = '';
    }
    var addHtml = '<div class="explanation-tree" id="explanation-' + trecId +
        '" style="width: 0; overflow: hidden;"><div style="width: 45em"><h5>' +
        '<button type="button" class="close" aria-label="Remove"><span aria-hidden="true">&times;</span></button>' +
        'Explanation for result <strong>' + trecId + '</strong></h5>' +
        ChatNoir.renderExplanationTree(jsonExplanation) + '</div></div>';
    body.html(addHtml + origHtml);

    var self = this;
    body.find('.explanation-tree .close').on('click', function () {
        $(this).parent().parent().parent().animate({'width': '0px', 'opacity': '0'}, 'normal', function () {
            $(this).remove();
            self.adjustExplanationPaneWidth();
            if (0 === body.find('.explanation-tree').length) {
                self.modal('hide');
            }
        });
    });

    body.find('.explanation-tree:first-child').animate({'width': '45em'}, 'normal', function () {
        self.adjustExplanationPaneWidth();
        $(this).find('> div').animate({'width': '100%'});
        $(this).addClass('active');

        body.find('.explanation-tree').not(':first-child').each(function () {
            $(this).removeClass('active');
        });
    })
};

$.fn.adjustExplanationPaneWidth = function () {
    var numPanes = 0;
    var contentPane = this.find('.modal-body > div');
    var modalWindow = this.find('.modal-lg');
    var trees = this.find('.explanation-tree');

    contentPane.css({'width': 'auto'});
    trees.css({'float': 'left'});
    trees.each(function () {
        ++numPanes;
    });

    if (1 >= numPanes) {
        modalWindow.animate({'width': '900px'});
        trees.css({'float': 'none', 'width': 'auto'});
    } else {
        contentPane.animate({'width': (numPanes * 47) + 'em'});
        trees.animate({'width': '45em'});
        modalWindow.animate({'width': '98%'});
    }
};


// --------------------------------------------------------------------
// Generic jQuery helpers
// --------------------------------------------------------------------

$.urlParam = function (name) {
    var results = new RegExp('[\?&]' + name + '(?:=([^&#]*))?').exec(window.location.href);
    if (null === results) {
        return null;
    }
    return results[1] || 0;
};
})();