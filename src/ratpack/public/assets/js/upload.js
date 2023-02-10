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
});