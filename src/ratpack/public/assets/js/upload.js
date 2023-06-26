$(function(){
    var loading = $('#loading');
    var MINIMUM_FILE_SIZE_KB = 16458;//The number of bytes in a kilobyte.
    var inputFile = $('#input-doc')[0].files[0];

    $('#doc-upload').on('submit', function(e){
       $('#msg-warning').addClass('hidden');
       $('#msg-warning2').addClass('hidden');
        if (!$('#input-doc').val()) {
          $('#msg-warning2').addClass('hidden');
          $('#msg-warning').removeClass('hidden');
          e.preventDefault();//prevent the page from reloading
        }else if (inputFile.size< MINIMUM_FILE_SIZE_KB) {
            if(inputFile.name.split('.').pop() != "txt"){
              $('#msg-warning').addClass('hidden');
              $('#msg-warning2').removeClass('hidden');
              e.preventDefault();//prevent the page from reloading
            }
        }else{
            $('#msg-warning').addClass('hidden');
            $('#msg-warning2').addClass('hidden');
            loading.show();
            console.log('Processing...');
            setTimeout(function(){
                $(loading).append("You will be redirected...");
            }, 5000);
        }
    });


    var $fileInput = $('#input-doc');

    $fileInput.on('change', function() {
        var fileInput = this.files[0];
        var $textContainer = $('#docInfo');
        var fileInfo = '';
        if (this.files.length === 1) {
            var fileName = fileInput.name;
            var fileSize = fileInput.size;
            var fileType = fileInput.type;

            fileInfo = '<b>File Name</b>: '+ fileName + '<br>' +
                '<b>File Size</b>: '+ formatBytes(fileSize) + '. <br>' +
                '<b>File Type</b>: '+ fileType;
            $textContainer.html(fileInfo);
        } else {
            $textContainer.html('<b>'+this.files.length + '</b> files selected');
        }
    });

});

function formatBytes(bytes, decimals = 2) {
    if (!+bytes) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'Kb', 'Mb', 'Gb', 'Tb'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    const fileSize = parseFloat((bytes / Math.pow(k, i)).toFixed(dm));
    return fileSize + ' ' + sizes[i];
}
