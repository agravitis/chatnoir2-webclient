/*!
 * ChatNoir 2 Web Frontend.
 * Copyright (C) 2014-2017 Janek Bevendorff, Webis Group
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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

$(function () {
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

    // initialize search settings
    $("#SearchIndices").SumoSelect({
        placeholder: 'Select indices',
        captionFormat:'{0} indices',
        captionFormatAllSelected:'All indices',
        forceCustomRendering: false,
        nativeOnDevice: ['Android', 'BlackBerry', 'iPhone', 'iPad', 'iPod', 'Opera Mini', 'IEMobile', 'Silk'],
        selectAll: true,
        csvDispCount: 2
    });

    var options = $("#SearchOptions");
    var optionsToggle = $("#SearchOptionsToggle").find("button");

    optionsToggle.click(function(e) {
        e.preventDefault();
        options.slideToggle(300);
        $(this).toggleClass("active");
    });

    // make cat purr when we are searching something
    var logo = $("#Logo");
    logo.load(function() {
        var searchInput = $("#SearchInput");
        var svgEyes = $(logo.get(0).contentDocument).find("#Eyes_1_");

        searchInput.keyup(function () {
            if (window.matchMedia('(max-width: 767px)').matches) {
                return;
            }

            if (searchInput.val().trim() !== "") {
                svgEyes.attr("visibility", "hidden");
            } else {
                svgEyes.attr("visibility", "visible");
            }
        });
        $(window).resize(function () {
            svgEyes.attr("visibility", "visible");
        });
    });
});

// --------------------------------------------------------------------
// Explanation Modal
// --------------------------------------------------------------------

ChatNoir.isExplainMode = function () {
    return null !== $.urlParam('explain');
};

ChatNoir.renderExplanationTree = function (explanation) {
    var htmlString = "";

    if (explanation instanceof Object && null !== explanation) {
        htmlString += '<ul><li><span class="key text-primary">' + explanation["description"] + '</span> ' +
            '<span class="text-muted"> = </span><span class="value">' + explanation["value"] + '</span>';

        if ("details" in explanation) {
            for (var i in explanation["details"]) {
                htmlString += ChatNoir.renderExplanationTree(explanation["details"][i]);
            }
        }

        htmlString += '</ul>';
    }

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
    var documentId = hash.substr(8);
    var jsonExplanation = $(hash).data('explanation');
    this.find('.modal-title').html('Explain search results');
    this.addExplanationTree(jsonExplanation, documentId);
    this.modal('show');
};

$.fn.addExplanationTree = function (jsonExplanation, documentId) {
    var body = this.find('.modal-body > div');
    body.find('#explanation-' + documentId).remove();
    this.adjustExplanationPaneWidth();

    var origHtml = body.html();
    if ('&nbsp;' === origHtml) {
        origHtml = '';
    }
    var addHtml = '<div class="explanation-tree" id="explanation-' + documentId +
        '" style="width: 0; overflow: hidden;"><div style="width: 45em"><h5>' +
        '<button type="button" class="close" aria-label="Remove"><span aria-hidden="true">&times;</span></button>' +
        'Explanation for result <strong>' + documentId + '</strong></h5>' +
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