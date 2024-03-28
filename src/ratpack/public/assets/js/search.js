$(function() {

  var directories = $('#directories');
  var folderIds = $('#folderId');
  var loading = $('#loading');
  var result = $('#result');
  var searchButton = $('#btn-search');
  var searchPattern = $('#pattern');
  var searchMaxHits = $('#maxHits');
  var documents = $('#documents');

  // $(loading).text('Loading directories...');

var enterKeyPress = function (e) {
  if(e.which == 13) {
    $(searchButton).click();
    return false;
  }
};

// $(loading).text('Loading directories...');

    $.getJSON('/api/list').success(function (data) {
      if ($(directories).find('li').size() === 0){
        $(data).each(function (i) {
          var folderId = data[i].id;
          directories.append('<li class="controller"><a href="/documents?folderId='+ folderId +'"><i class="glyphicon glyphicon-folder-open"> </i> '+data[i].name+'</a></li>');
          folderIds.append('<option value="'+ folderId +'"> - '+data[i].name+'</option>');
        });
      }
    }).fail(function() {
    //  $(loading).text('Error request!');
      $(loading).hide();
    }).done(function(data) {
      // $(loading).text('Directories');
    });

    $(searchPattern).keypress(enterKeyPress);
    $(folderIds).change(function () {
      $(searchButton).click();
    });

  $(searchButton).click(function () {
    $.getJSON('/api/search', {
      'pattern': $(searchPattern).val(),
      'folderId': $(folderIds).val()
      /*'maxHits': $(searchMaxHits).val()*/
    }).success(function (data){
      $(documents).empty();
      var rows = data.hits;
      if (rows.length > 0){
        for (var i = 0; i < rows.length; i++) {
          var doc = rows[i];
          documents.append('<li><i class="glyphicon glyphicon-file"></i> <a href="/document?id='+ doc.id +'">'+doc.fileName+'</a> (version: '+doc.version+')</li>');
          documents.append('<p><strong>'+ doc.tags.join(', ')+'</strong></p>');
          documents.append('<p>'+ doc.summary+'</p>');
          // console.log(doc.attributes);
        }
      }
    }).fail(function() {
      $(result).text('Error request!');
    }).done(function(data) {
      $(result).text( 'Result of search: ' + data.hits.length );
    });

  });

});
