var directories = $('#directories').val();
    var dir = JSON.parse(directories);
    var columns = [{
      field: 'id',
      title: 'ID'
    }, {
      field: 'parentId',
      title: 'parent ID'
    }, {
      field: 'name',
      title: 'Name'
    }, {
      field: 'lastModified',
      title: 'Last Modified'
    }, {
      field: 'creator',
      title: 'Creator'
    }];
$(document).ready(function() {
    $('#table').bootstrapTable({
       toggle: 'table',
       search: true,//enables a search box above the table
       searchable: true,//enables searching for all columns
       pagination: true,
       sidePagination: 'client',//all the data will be loaded on the client-side
       columns: columns ,
       data:dir
    });
});

